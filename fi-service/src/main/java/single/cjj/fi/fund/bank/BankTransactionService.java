package single.cjj.fi.fund.bank;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.exception.BizException;
import single.cjj.fi.fund.bank.BankTransactionContracts.CreateRequest;
import single.cjj.fi.fund.bank.BankTransactionContracts.Detail;
import single.cjj.fi.fund.bank.BankTransactionContracts.Difference;
import single.cjj.fi.fund.bank.BankTransactionContracts.MatchRequest;
import single.cjj.fi.fund.bank.BankTransactionContracts.MatchResult;
import single.cjj.fi.fund.bank.BankTransactionRepository.BankTransactionRow;
import single.cjj.fi.fund.bank.BankTransactionRepository.ReconciliationBatchRow;
import single.cjj.fi.fund.bank.BankTransactionRepository.ReconciliationCaseRow;
import single.cjj.fi.fund.bank.BankTransactionRepository.ReconciliationDifferenceRow;
import single.cjj.fi.fund.bank.BankTransactionRepository.ReconciliationMatchRow;
import single.cjj.fi.fund.bank.BankTransactionRepository.ReconciliationParticipantRow;
import single.cjj.fi.fund.payment.PaymentOrderRepository;
import single.cjj.fi.fund.payment.PaymentOrderRepository.PaymentOrderRow;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class BankTransactionService {

    private final BankTransactionRepository repository;
    private final PaymentOrderRepository paymentOrderRepository;
    private final ObjectMapper objectMapper;
    private final BankPaymentMatchEvaluator evaluator = new BankPaymentMatchEvaluator();

    public BankTransactionService(
            BankTransactionRepository repository,
            PaymentOrderRepository paymentOrderRepository,
            ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.paymentOrderRepository = paymentOrderRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public Detail create(CreateRequest request, Long operatorId) {
        BankTransactionRow existing = repository.findByNaturalKey(
                request.tenantId(), request.bankAccountId(), request.bankTransactionNo());
        if (existing != null) {
            return toDetail(existing);
        }

        String rawJson = jsonOrNull(request.rawPayload());
        String rawHash = normalizeText(request.rawPayloadHash());
        if (rawHash == null && rawJson != null) {
            rawHash = sha256(rawJson);
        }

        LocalDateTime now = LocalDateTime.now();
        BankTransactionRow row = new BankTransactionRow(
                IdWorker.getId(),
                request.tenantId(),
                request.orgId(),
                normalizeRequired(request.bankAccountId(), "银行账户"),
                normalizeRequired(request.bankTransactionNo(), "银行流水号"),
                request.transactionDate(),
                request.transactionTime(),
                normalizeDirection(request.direction()),
                normalizeRequired(request.currencyCode(), "币种").toUpperCase(),
                money(request.amount()),
                normalizeText(request.counterpartyName()),
                normalizeText(request.counterpartyAccount()),
                normalizeText(request.purpose()),
                normalizeText(request.summary()),
                normalizeText(request.bankReceiptNo()),
                normalizeRequired(request.sourceChannel(), "来源渠道"),
                "UNMATCHED",
                "CONFIRMED",
                null,
                null,
                null,
                rawHash,
                rawJson,
                operatorId,
                now,
                0
        );
        try {
            repository.insert(row);
        } catch (DuplicateKeyException duplicateKeyException) {
            existing = repository.findByNaturalKey(
                    request.tenantId(), request.bankAccountId(), request.bankTransactionNo());
            if (existing != null) {
                return toDetail(existing);
            }
            throw duplicateKeyException;
        }
        return toDetail(repository.find(row.id(), row.tenantId()));
    }

    public Detail detail(Long fid, String tenantId) {
        BankTransactionRow row = repository.find(fid, tenantId);
        if (row == null) {
            throw new BizException("银行流水不存在: " + fid);
        }
        return toDetail(row);
    }

    public List<Detail> list(
            String tenantId,
            Long orgId,
            String matchStatus,
            int limit
    ) {
        return repository.list(tenantId, orgId, matchStatus, limit)
                .stream()
                .map(this::toDetail)
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public MatchResult matchPaymentOrder(
            Long bankTransactionId,
            String tenantId,
            MatchRequest request
    ) {
        BankTransactionRow bank = repository.lock(bankTransactionId, tenantId);
        if (bank == null) {
            throw new BizException("银行流水不存在: " + bankTransactionId);
        }
        if (!"CONFIRMED".equals(bank.status())) {
            throw new BizException("只有已确认银行流水可以执行付款匹配");
        }
        if ("MATCHED".equals(bank.matchStatus())) {
            throw new BizException("银行流水已经完成匹配");
        }

        PaymentOrderRow order = paymentOrderRepository.lockOrder(
                request.paymentOrderId(), tenantId);
        if (order == null) {
            throw new BizException("付款单不存在: " + request.paymentOrderId());
        }
        if (!List.of("AUDITED", "PAYING").contains(order.status())) {
            throw new BizException("只有已审核或支付中的付款单可以匹配银行流水");
        }
        if ("MATCHED".equals(order.bankMatchStatus())) {
            throw new BizException("付款单已经完成银行流水匹配");
        }
        if (!order.orgId().equals(bank.orgId())) {
            throw new BizException("银行流水组织与付款单不一致");
        }

        List<Difference> differences = evaluator.evaluate(order, bank);
        String result = differences.isEmpty() ? "MATCHED" : "DIFFERENCE";
        LocalDateTime now = LocalDateTime.now();

        Long batchId = IdWorker.getId();
        Long caseId = IdWorker.getId();
        String batchNo = "RB-BANK-" + suffix(batchId);
        String caseNo = "RC-BANK-" + suffix(caseId);
        String requestId = "BANKPAY:"
                + tenantId + ":" + bank.id() + ":" + order.id() + ":"
                + UUID.randomUUID().toString().replace("-", "");

        repository.insertReconciliationBatch(new ReconciliationBatchRow(
                batchId, tenantId, order.orgId(), batchNo, requestId,
                bank.id(), bank.bankTransactionNo(), result, now, now
        ));

        Map<String, Object> caseSnapshot = new LinkedHashMap<>();
        caseSnapshot.put("paymentOrderId", order.id());
        caseSnapshot.put("paymentOrderNo", order.number());
        caseSnapshot.put("bankTransactionId", bank.id());
        caseSnapshot.put("bankTransactionNo", bank.bankTransactionNo());
        caseSnapshot.put("result", result);
        repository.insertReconciliationCase(new ReconciliationCaseRow(
                caseId, tenantId, order.orgId(), batchId, caseNo,
                "PAYMENT_ORDER:" + order.id() + "|BANK:" + bank.id(),
                result, json(caseSnapshot), now
        ));

        repository.insertParticipant(new ReconciliationParticipantRow(
                IdWorker.getId(), tenantId, order.orgId(), caseId,
                "PAYMENT_ORDER", "FI_PAYMENT_ORDER",
                String.valueOf(order.id()), order.number(), order.currencyCode(),
                order.date(), json(paymentSnapshot(order)), now
        ));
        repository.insertParticipant(new ReconciliationParticipantRow(
                IdWorker.getId(), tenantId, bank.orgId(), caseId,
                "BANK_TRANSACTION", "FI_BANK_TRANSACTION",
                String.valueOf(bank.id()), bank.bankTransactionNo(), bank.currencyCode(),
                bank.transactionDate(), json(bankSnapshot(bank)), now
        ));

        repository.insertMatch(new ReconciliationMatchRow(
                IdWorker.getId(), tenantId, order.orgId(), caseId,
                json(paymentSnapshot(order)), json(bankSnapshot(bank)),
                result, now
        ));

        for (Difference difference : differences) {
            repository.insertDifference(new ReconciliationDifferenceRow(
                    IdWorker.getId(), tenantId, order.orgId(), caseId,
                    difference.code(), difference.field(),
                    difference.expected(), difference.actual(),
                    difference.message(), now
            ));
        }

        Long matchedOrderId = "MATCHED".equals(result) ? order.id() : null;
        repository.updateMatch(
                bank.id(), tenantId, result, matchedOrderId,
                batchId, caseId, request.operatorId());
        paymentOrderRepository.updateOrderBankMatch(
                order.id(), tenantId, result, batchId, caseId, request.operatorId());

        return new MatchResult(
                bank.id(), order.id(), result, batchId, caseId, differences
        );
    }

    private Map<String, Object> paymentSnapshot(PaymentOrderRow order) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("paymentOrderId", order.id());
        value.put("paymentOrderNo", order.number());
        value.put("status", order.status());
        value.put("payerBankAccountId", order.payerBankAccountId());
        value.put("payeeBankAccountNo", order.payeeBankAccountNo());
        value.put("currencyCode", order.currencyCode());
        value.put("amount", money(order.amount()));
        return value;
    }

    private Map<String, Object> bankSnapshot(BankTransactionRow bank) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("bankTransactionId", bank.id());
        value.put("bankTransactionNo", bank.bankTransactionNo());
        value.put("direction", bank.direction());
        value.put("bankAccountId", bank.bankAccountId());
        value.put("counterpartyAccount", bank.counterpartyAccount());
        value.put("currencyCode", bank.currencyCode());
        value.put("amount", money(bank.amount()));
        return value;
    }

    private Detail toDetail(BankTransactionRow row) {
        return new Detail(
                row.id(), row.tenantId(), row.orgId(), row.bankAccountId(),
                row.bankTransactionNo(), row.transactionDate(), row.transactionTime(),
                row.direction(), row.currencyCode(), row.amount(),
                row.counterpartyName(), row.counterpartyAccount(),
                row.purpose(), row.summary(), row.bankReceiptNo(),
                row.sourceChannel(), row.matchStatus(), row.status(),
                row.matchedPaymentOrderId(), row.reconciliationBatchId(),
                row.reconciliationCaseId(), row.rawPayloadHash(), row.version()
        );
    }

    private String normalizeDirection(String value) {
        String direction = normalizeRequired(value, "银行流水方向").toUpperCase();
        if (!List.of("OUTBOUND", "INBOUND").contains(direction)) {
            throw new BizException("银行流水方向不支持: " + value);
        }
        return direction;
    }

    private BigDecimal money(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeRequired(String value, String field) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(field + "不能为空");
        }
        return value.trim();
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String suffix(Long id) {
        String value = String.valueOf(id);
        return value.length() <= 10 ? value : value.substring(value.length() - 10);
    }

    private String jsonOrNull(Object value) {
        return value == null ? null : json(value);
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BizException("银行匹配快照序列化失败");
        }
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}
