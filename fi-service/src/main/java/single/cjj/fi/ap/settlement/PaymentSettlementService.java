package single.cjj.fi.ap.settlement;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import single.cjj.bizfi.exception.BizException;
import single.cjj.fi.ap.settlement.PaymentSettlementContracts.Detail;
import single.cjj.fi.ap.settlement.PaymentSettlementContracts.EntryView;
import single.cjj.fi.ap.settlement.PaymentSettlementContracts.ListItem;
import single.cjj.fi.ap.settlement.PaymentSettlementRepository.ApplicationAllocationRow;
import single.cjj.fi.ap.settlement.PaymentSettlementRepository.BankTransactionRow;
import single.cjj.fi.ap.settlement.PaymentSettlementRepository.OrderAllocationRow;
import single.cjj.fi.ap.settlement.PaymentSettlementRepository.PayableRow;
import single.cjj.fi.ap.settlement.PaymentSettlementRepository.PaymentApplicationRow;
import single.cjj.fi.ap.settlement.PaymentSettlementRepository.PaymentOrderRow;
import single.cjj.fi.ap.settlement.PaymentSettlementRepository.SettlementEntryRow;
import single.cjj.fi.ap.settlement.PaymentSettlementRepository.SettlementRow;
import single.cjj.fi.event.FiBusinessEventOutboxEntity;
import single.cjj.fi.event.FiBusinessEventOutboxService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class PaymentSettlementService {

    public static final String EVENT_PAYMENT_COMPLETED = "PAYMENT_COMPLETED";
    public static final String ROUTING_PAYMENT_COMPLETED = "biz.finance.payment.completed";

    private final PaymentSettlementRepository repository;
    private final FiBusinessEventOutboxService outboxService;

    public PaymentSettlementService(
            PaymentSettlementRepository repository,
            FiBusinessEventOutboxService outboxService
    ) {
        this.repository = repository;
        this.outboxService = outboxService;
    }

    @Transactional(rollbackFor = Exception.class)
    public Detail finalizePayment(
            Long paymentOrderId,
            String tenantId,
            Long operatorId
    ) {
        SettlementRow existing = repository.findByPaymentOrder(paymentOrderId, tenantId);
        if (existing != null) {
            return detail(existing.id(), tenantId);
        }

        PaymentOrderRow order = repository.lockPaymentOrder(paymentOrderId, tenantId);
        if (order == null) {
            throw new BizException("付款单不存在: " + paymentOrderId);
        }

        existing = repository.findByPaymentOrder(paymentOrderId, tenantId);
        if (existing != null) {
            return detail(existing.id(), tenantId);
        }

        validateOrder(order);

        BankTransactionRow bank = repository.lockMatchedBankTransaction(
                paymentOrderId, tenantId);
        validateBank(order, bank);

        List<OrderAllocationRow> orderAllocations =
                repository.lockOrderAllocations(paymentOrderId, tenantId);
        validateOrderAllocations(order, orderAllocations);

        Long settlementId = IdWorker.getId();
        LocalDate settlementDate = bank.transactionDate();
        LocalDateTime now = LocalDateTime.now();
        SettlementRow settlement = new SettlementRow(
                settlementId,
                tenantId,
                order.orgId(),
                settlementNumber(settlementId, settlementDate),
                order.id(),
                bank.id(),
                order.businessPartnerId(),
                order.businessPartnerCode(),
                order.businessPartnerName(),
                order.currencyCode(),
                money(order.amount()),
                "COMPLETED",
                settlementDate,
                null,
                null,
                null,
                null,
                operatorId,
                now,
                0
        );

        try {
            repository.insertSettlement(settlement);
        } catch (DuplicateKeyException duplicateKeyException) {
            SettlementRow duplicate =
                    repository.findByPaymentOrder(paymentOrderId, tenantId);
            if (duplicate != null) {
                return detail(duplicate.id(), tenantId);
            }
            throw duplicateKeyException;
        }

        List<SettlementEntryRow> settlementEntries = new ArrayList<>();
        BigDecimal totalSettled = BigDecimal.ZERO.setScale(2);

        for (OrderAllocationRow orderAllocation : orderAllocations) {
            PaymentApplicationRow application = repository.lockApplication(
                    orderAllocation.paymentApplicationId(), tenantId);
            validateApplication(order, application);

            BigDecimal remaining = money(orderAllocation.amount());
            List<ApplicationAllocationRow> applicationAllocations =
                    repository.lockAvailableApplicationAllocations(
                            application.id(), tenantId);

            for (ApplicationAllocationRow appAllocation : applicationAllocations) {
                if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }

                BigDecimal available = money(
                        nz(appAllocation.reservedAmount())
                                .subtract(nz(appAllocation.consumedAmount())));
                if (available.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                BigDecimal settleAmount = remaining.min(available);
                PayableRow payable = repository.lockPayable(
                        appAllocation.payableId(), tenantId);
                validatePayable(order, application, payable, settleAmount);

                BigDecimal originalOpen = money(payable.openAmount());
                BigDecimal originalReserved = money(payable.reservedAmount());
                BigDecimal remainingOpen = money(originalOpen.subtract(settleAmount));
                BigDecimal remainingReserved = money(
                        originalReserved.subtract(settleAmount));
                BigDecimal newSettled = money(
                        nz(payable.settledAmount()).add(settleAmount));

                repository.updatePayableBalances(
                        payable.id(),
                        tenantId,
                        newSettled,
                        remainingOpen,
                        remainingReserved,
                        remainingOpen.compareTo(BigDecimal.ZERO) == 0
                                ? "SETTLED"
                                : "PARTIAL_SETTLED",
                        operatorId
                );

                BigDecimal newConsumed = money(
                        nz(appAllocation.consumedAmount()).add(settleAmount));
                String allocationStatus =
                        newConsumed.compareTo(money(appAllocation.reservedAmount())) == 0
                                ? "CONSUMED"
                                : "RESERVED";
                repository.updateApplicationAllocationConsumed(
                        appAllocation.id(),
                        tenantId,
                        newConsumed,
                        allocationStatus,
                        operatorId
                );

                SettlementEntryRow entry = new SettlementEntryRow(
                        IdWorker.getId(),
                        tenantId,
                        order.orgId(),
                        settlementId,
                        payable.id(),
                        payable.number(),
                        application.id(),
                        appAllocation.id(),
                        orderAllocation.id(),
                        settleAmount,
                        originalOpen,
                        remainingOpen,
                        originalReserved,
                        remainingReserved,
                        "COMPLETED",
                        operatorId,
                        LocalDateTime.now(),
                        0
                );
                repository.insertSettlementEntry(entry);
                settlementEntries.add(entry);

                remaining = money(remaining.subtract(settleAmount));
                totalSettled = money(totalSettled.add(settleAmount));
            }

            if (remaining.compareTo(BigDecimal.ZERO) != 0) {
                throw new BizException(
                        "付款申请有效应付占用不足，无法完成付款核销: "
                                + application.number()
                                + ", remaining="
                                + remaining.toPlainString());
            }

            repository.markOrderAllocationConsumed(
                    orderAllocation.id(), tenantId, operatorId);
        }

        if (totalSettled.compareTo(money(order.amount())) != 0) {
            throw new BizException(
                    "付款核销金额与付款单金额不一致: settlement="
                            + totalSettled.toPlainString()
                            + ", order="
                            + money(order.amount()).toPlainString());
        }

        repository.markPaymentOrderPaid(
                order.id(), tenantId, settlementId, operatorId);

        FiBusinessEventOutboxEntity businessEvent = outboxService.append(
                tenantId,
                order.orgId(),
                EVENT_PAYMENT_COMPLETED,
                "FUND",
                "PAYMENT_ORDER",
                order.id(),
                order.version() == null ? 1L : order.version().longValue() + 1L,
                "FI_PAYMENT_ORDER",
                order.number(),
                settlementDate,
                order.reconciliationCaseId() == null
                        ? null
                        : String.valueOf(order.reconciliationCaseId()),
                bank.bankTransactionNo(),
                null,
                operatorId,
                ROUTING_PAYMENT_COMPLETED,
                buildPaymentCompletedPayload(
                        order,
                        bank,
                        settlementId,
                        settlement.number(),
                        settlementDate,
                        settlementEntries
                )
        );

        repository.updateSettlementBusinessEvent(
                settlementId,
                tenantId,
                businessEvent.getFeventId(),
                operatorId
        );

        return detail(settlementId, tenantId);
    }

    public List<ListItem> list(
            String tenantId,
            Long orgId,
            String status,
            int limit
    ) {
        return repository.listSettlements(tenantId, orgId, status, limit)
                .stream()
                .map(row -> new ListItem(
                        row.id(),
                        row.tenantId(),
                        row.orgId(),
                        row.number(),
                        row.paymentOrderId(),
                        row.bankTransactionId(),
                        row.businessPartnerId(),
                        row.businessPartnerCode(),
                        row.businessPartnerName(),
                        row.currencyCode(),
                        row.amount(),
                        row.status(),
                        row.settlementDate(),
                        row.businessEventId(),
                        row.accountingEventId(),
                        row.voucherId(),
                        row.voucherNumber(),
                        row.createTime()
                ))
                .toList();
    }

    public Detail detail(Long settlementId, String tenantId) {
        SettlementRow settlement = repository.findSettlement(settlementId, tenantId);
        if (settlement == null) {
            throw new BizException("应付付款核销不存在: " + settlementId);
        }
        List<EntryView> entries = repository.findSettlementEntries(
                        settlementId, tenantId)
                .stream()
                .map(item -> new EntryView(
                        item.id(),
                        item.payableId(),
                        item.payableNumber(),
                        item.paymentApplicationId(),
                        item.paymentApplicationAllocationId(),
                        item.paymentOrderAllocationId(),
                        item.settledAmount(),
                        item.originalOpenAmount(),
                        item.remainingOpenAmount(),
                        item.originalReservedAmount(),
                        item.remainingReservedAmount(),
                        item.status()
                ))
                .toList();
        return new Detail(
                settlement.id(),
                settlement.tenantId(),
                settlement.orgId(),
                settlement.number(),
                settlement.paymentOrderId(),
                settlement.bankTransactionId(),
                settlement.businessPartnerId(),
                settlement.businessPartnerCode(),
                settlement.businessPartnerName(),
                settlement.currencyCode(),
                settlement.amount(),
                settlement.status(),
                settlement.settlementDate(),
                settlement.businessEventId(),
                settlement.accountingEventId(),
                settlement.voucherId(),
                settlement.voucherNumber(),
                settlement.createTime(),
                entries
        );
    }

    private void validateOrder(PaymentOrderRow order) {
        if (!List.of("AUDITED", "PAYING").contains(order.status())) {
            throw new BizException(
                    "只有已审核或支付中的付款单允许完成支付核销");
        }
        if (!"AUDITED".equals(order.approvalStatus())) {
            throw new BizException("付款单未完成审核");
        }
        if (!"MATCHED".equals(order.bankMatchStatus())) {
            throw new BizException("付款单银行流水尚未匹配成功");
        }
        if (order.reconciliationCaseId() == null) {
            throw new BizException("付款单缺少 BANK_PAYMENT Reconciliation Case");
        }
        if (money(order.amount()).compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("付款单金额必须大于0");
        }
    }

    private void validateBank(
            PaymentOrderRow order,
            BankTransactionRow bank
    ) {
        if (bank == null) {
            throw new BizException("付款单缺少已匹配银行流水");
        }
        if (!Objects.equals(bank.matchedPaymentOrderId(), order.id())) {
            throw new BizException("银行流水与付款单匹配关系异常");
        }
        if (!"MATCHED".equals(bank.matchStatus())
                || !"CONFIRMED".equals(bank.status())) {
            throw new BizException("银行流水未处于已确认匹配状态");
        }
        if (!"OUTBOUND".equals(bank.direction())) {
            throw new BizException("支付完成流水必须为 OUTBOUND");
        }
        if (!Objects.equals(bank.orgId(), order.orgId())) {
            throw new BizException("银行流水组织与付款单不一致");
        }
        if (!Objects.equals(bank.currencyCode(), order.currencyCode())) {
            throw new BizException("银行流水币种与付款单不一致");
        }
        if (money(bank.amount()).compareTo(money(order.amount())) != 0) {
            throw new BizException("银行流水金额与付款单不一致");
        }
        if (!Objects.equals(bank.bankAccountId(), order.payerBankAccountId())) {
            throw new BizException("银行流水账户与付款方账户不一致");
        }
    }

    private void validateOrderAllocations(
            PaymentOrderRow order,
            List<OrderAllocationRow> allocations
    ) {
        if (allocations.isEmpty()) {
            throw new BizException("付款单缺少付款申请分配");
        }
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (OrderAllocationRow allocation : allocations) {
            if (!"ORDERED".equals(allocation.status())) {
                throw new BizException(
                        "付款单分配状态不允许完成核销: "
                                + allocation.id()
                                + "/"
                                + allocation.status());
            }
            total = money(total.add(allocation.amount()));
        }
        if (total.compareTo(money(order.amount())) != 0) {
            throw new BizException(
                    "付款单分配总额与付款单金额不一致: "
                            + total.toPlainString());
        }
    }

    private void validateApplication(
            PaymentOrderRow order,
            PaymentApplicationRow application
    ) {
        if (application == null) {
            throw new BizException("付款单关联付款申请不存在");
        }
        if (!Objects.equals(application.orgId(), order.orgId())) {
            throw new BizException("付款申请组织与付款单不一致");
        }
        if (!Objects.equals(application.businessPartnerId(), order.businessPartnerId())) {
            throw new BizException("付款申请供应商与付款单不一致");
        }
        if (!Objects.equals(application.currencyCode(), order.currencyCode())) {
            throw new BizException("付款申请币种与付款单不一致");
        }
        if (!"APPROVED".equals(application.status())
                || !"AUDITED".equals(application.approvalStatus())) {
            throw new BizException(
                    "付款申请不处于已审批状态: " + application.number());
        }
    }

    private void validatePayable(
            PaymentOrderRow order,
            PaymentApplicationRow application,
            PayableRow payable,
            BigDecimal settleAmount
    ) {
        if (payable == null) {
            throw new BizException("付款申请关联应付单不存在");
        }
        if (!"FORMAL".equals(payable.type())) {
            throw new BizException(
                    "只有正式应付允许付款核销: " + payable.number());
        }
        if (!List.of("OPEN", "PARTIAL_SETTLED").contains(payable.status())) {
            throw new BizException(
                    "应付单状态不允许付款核销: "
                            + payable.number()
                            + "/"
                            + payable.status());
        }
        if (!"AUDITED".equals(payable.approvalStatus())) {
            throw new BizException("应付单未审核: " + payable.number());
        }
        if (!List.of("VOUCHER_GENERATED", "POSTED")
                .contains(payable.accountingStatus())) {
            throw new BizException("应付挂账核算尚未完成: " + payable.number());
        }
        if (!Objects.equals(payable.orgId(), order.orgId())
                || !Objects.equals(payable.businessPartnerId(), application.businessPartnerId())
                || !Objects.equals(payable.currencyCode(), order.currencyCode())) {
            throw new BizException("应付单组织/供应商/币种与付款链不一致");
        }
        if (money(payable.openAmount()).compareTo(settleAmount) < 0) {
            throw new BizException(
                    "应付未核销余额不足: " + payable.number());
        }
        if (money(payable.reservedAmount()).compareTo(settleAmount) < 0) {
            throw new BizException(
                    "应付付款申请占用不足: " + payable.number());
        }
    }

    private Map<String, Object> buildPaymentCompletedPayload(
            PaymentOrderRow order,
            BankTransactionRow bank,
            Long settlementId,
            String settlementNo,
            LocalDate settlementDate,
            List<SettlementEntryRow> entries
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("paymentOrderId", order.id());
        payload.put("paymentOrderNo", order.number());
        payload.put("settlementId", settlementId);
        payload.put("settlementNo", settlementNo);
        payload.put("bankTransactionId", bank.id());
        payload.put("bankTransactionNo", bank.bankTransactionNo());
        payload.put("businessPartnerId", order.businessPartnerId());
        payload.put("businessPartnerCode", order.businessPartnerCode());
        payload.put("businessPartnerName", order.businessPartnerName());
        payload.put("currencyCode", order.currencyCode());
        payload.put("amount", money(order.amount()));
        payload.put("payerBankAccountId", order.payerBankAccountId());
        payload.put("settlementDate", settlementDate);
        payload.put("entries", entries.stream().map(entry -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("settlementEntryId", entry.id());
            item.put("payableId", entry.payableId());
            item.put("payableNumber", entry.payableNumber());
            item.put("paymentApplicationId", entry.paymentApplicationId());
            item.put("settledAmount", entry.settledAmount());
            return item;
        }).toList());
        return payload;
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal money(BigDecimal value) {
        return nz(value).setScale(2, RoundingMode.HALF_UP);
    }

    private String settlementNumber(Long id, LocalDate date) {
        String value = String.valueOf(id);
        String suffix = value.length() <= 8
                ? value
                : value.substring(value.length() - 8);
        return "APSET-"
                + date.toString().replace("-", "")
                + "-"
                + suffix;
    }
}
