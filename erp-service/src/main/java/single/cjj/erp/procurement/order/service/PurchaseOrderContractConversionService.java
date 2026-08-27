package single.cjj.erp.procurement.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.exception.BizException;
import single.cjj.erp.procurement.contract.entity.PurchaseContractEntity;
import single.cjj.erp.procurement.contract.entity.PurchaseContractEntryEntity;
import single.cjj.erp.procurement.contract.mapper.PurchaseContractEntryMapper;
import single.cjj.erp.procurement.contract.mapper.PurchaseContractMapper;
import single.cjj.erp.procurement.order.dto.PurchaseOrderContracts.PurchaseOrderDetail;
import single.cjj.erp.procurement.order.dto.PurchaseOrderContracts.PurchaseOrderFromContractCreateRequest;
import single.cjj.erp.procurement.order.dto.PurchaseOrderContracts.PurchaseOrderFromContractEntryRequest;
import single.cjj.erp.procurement.order.entity.PurchaseOrderEntity;
import single.cjj.erp.procurement.order.entity.PurchaseOrderEntryEntity;
import single.cjj.erp.procurement.order.mapper.PurchaseOrderEntryMapper;
import single.cjj.erp.procurement.order.mapper.PurchaseOrderMapper;
import single.cjj.erp.procurement.request.entity.PurchaseRequestEntity;
import single.cjj.erp.procurement.request.entity.PurchaseRequestEntryEntity;
import single.cjj.erp.procurement.request.mapper.PurchaseRequestEntryMapper;
import single.cjj.erp.procurement.request.mapper.PurchaseRequestMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class PurchaseOrderContractConversionService {

    private static final String CONTRACT_EFFECTIVE = "EFFECTIVE";
    private static final String CONTRACT_APPROVED = "APPROVED";
    private static final String EXECUTION_NONE = "NONE";
    private static final String EXECUTION_SOURCING = "SOURCING";
    private static final String EXECUTION_CONTRACTING = "CONTRACTING";
    private static final String EXECUTION_ORDERING = "ORDERING";
    private static final String EXECUTION_COMPLETE = "COMPLETE";

    private final PurchaseOrderMapper orderMapper;
    private final PurchaseOrderEntryMapper orderEntryMapper;
    private final PurchaseContractMapper contractMapper;
    private final PurchaseContractEntryMapper contractEntryMapper;
    private final PurchaseRequestMapper requestMapper;
    private final PurchaseRequestEntryMapper requestEntryMapper;

    public PurchaseOrderContractConversionService(
            PurchaseOrderMapper orderMapper,
            PurchaseOrderEntryMapper orderEntryMapper,
            PurchaseContractMapper contractMapper,
            PurchaseContractEntryMapper contractEntryMapper,
            PurchaseRequestMapper requestMapper,
            PurchaseRequestEntryMapper requestEntryMapper
    ) {
        this.orderMapper = orderMapper;
        this.orderEntryMapper = orderEntryMapper;
        this.contractMapper = contractMapper;
        this.contractEntryMapper = contractEntryMapper;
        this.requestMapper = requestMapper;
        this.requestEntryMapper = requestEntryMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrderDetail createFromContract(
            PurchaseOrderFromContractCreateRequest request,
            Long operatorId
    ) {
        String tenantId = requireTenant(request.ftenantId());
        PurchaseContractEntity contract =
                contractMapper.selectByIdForUpdate(request.fcontractId(), tenantId);
        if (contract == null) {
            throw new BizException("采购合同不存在: " + request.fcontractId());
        }
        if (!CONTRACT_EFFECTIVE.equals(contract.getFstatus())
                || !CONTRACT_APPROVED.equals(contract.getFapprovalStatus())) {
            throw new BizException("只有已审批生效采购合同允许生成采购订单");
        }

        LocalDate orderDate = request.fdate() == null ? LocalDate.now() : request.fdate();
        validateContractDate(contract, orderDate);
        Long orderId = IdWorker.getId();
        String number = StringUtils.hasText(request.fnumber())
                ? request.fnumber().trim()
                : buildNumber(orderDate, orderId);
        ensureNumberUnique(tenantId, number);

        List<PurchaseOrderFromContractEntryRequest> requested =
                new ArrayList<>(request.entries());
        requested.sort(Comparator.comparing(
                PurchaseOrderFromContractEntryRequest::fcontractEntryId));

        Set<Long> seenContractEntries = new LinkedHashSet<>();
        Set<Long> affectedRequestIds = new LinkedHashSet<>();
        List<PurchaseOrderEntryEntity> orderEntries = new ArrayList<>();
        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal netTotal = BigDecimal.ZERO;
        BigDecimal taxTotal = BigDecimal.ZERO;
        BigDecimal grossTotal = BigDecimal.ZERO;
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < requested.size(); i++) {
            PurchaseOrderFromContractEntryRequest item = requested.get(i);
            if (!seenContractEntries.add(item.fcontractEntryId())) {
                throw new BizException("同一采购订单不能重复引用同一合同分录: "
                        + item.fcontractEntryId());
            }

            PurchaseContractEntryEntity contractEntry =
                    contractEntryMapper.selectByIdForUpdate(
                            item.fcontractEntryId(), tenantId);
            if (contractEntry == null
                    || !contract.getFid().equals(contractEntry.getFpurchaseContractId())) {
                throw new BizException("采购合同分录不存在或归属不匹配: "
                        + item.fcontractEntryId());
            }

            BigDecimal contractRemaining =
                    nz(contractEntry.getFquantity())
                            .subtract(nz(contractEntry.getForderedQuantity()));
            if (item.fquantity().compareTo(contractRemaining) > 0) {
                throw new BizException("采购订单数量超过合同剩余可下单数量: contractEntry="
                        + contractEntry.getFid() + ", remaining=" + contractRemaining);
            }

            PurchaseRequestEntryEntity requestEntry =
                    requestEntryMapper.selectByIdForUpdate(
                            contractEntry.getFpurchaseRequestEntryId(), tenantId);
            if (requestEntry == null
                    || !contractEntry.getFpurchaseRequestId()
                    .equals(requestEntry.getFpurchaseRequestId())) {
                throw new BizException("来源采购申请分录不存在: "
                        + contractEntry.getFpurchaseRequestEntryId());
            }

            BigDecimal requestRemaining =
                    nz(requestEntry.getFquantity())
                            .subtract(nz(requestEntry.getForderedQuantity()));
            if (item.fquantity().compareTo(requestRemaining) > 0) {
                throw new BizException("采购订单数量超过采购申请剩余可下单数量: requestEntry="
                        + requestEntry.getFid() + ", remaining=" + requestRemaining);
            }

            BigDecimal quantity = item.fquantity();
            BigDecimal net = money(quantity.multiply(contractEntry.getFunitPrice()));
            BigDecimal tax = money(net.multiply(nz(contractEntry.getFtaxRate())));
            BigDecimal gross = money(net.add(tax));

            PurchaseOrderEntryEntity orderEntry = new PurchaseOrderEntryEntity();
            orderEntry.setFid(IdWorker.getId());
            orderEntry.setFtenantId(tenantId);
            orderEntry.setForgId(contract.getForgId());
            orderEntry.setFpurchaseOrderId(orderId);
            orderEntry.setFcontractEntryId(contractEntry.getFid());
            orderEntry.setFsourcingAwardEntryId(
                    contractEntry.getFsourcingAwardEntryId());
            orderEntry.setFrfqEntryId(contractEntry.getFrfqEntryId());
            orderEntry.setFpurchaseRequestId(contractEntry.getFpurchaseRequestId());
            orderEntry.setFpurchaseRequestEntryId(
                    contractEntry.getFpurchaseRequestEntryId());
            orderEntry.setFlineNo(i + 1);
            orderEntry.setFmaterialId(contractEntry.getFmaterialId());
            orderEntry.setFmaterialCode(contractEntry.getFmaterialCode());
            orderEntry.setFmaterialName(contractEntry.getFmaterialName());
            orderEntry.setFspecification(contractEntry.getFspecification());
            orderEntry.setFunitId(contractEntry.getFunitId());
            orderEntry.setFquantity(quantity);
            orderEntry.setFunitPrice(contractEntry.getFunitPrice());
            orderEntry.setFnetAmount(net);
            orderEntry.setFtaxRate(nz(contractEntry.getFtaxRate()));
            orderEntry.setFtaxAmount(tax);
            orderEntry.setFgrossAmount(gross);
            orderEntry.setFplannedDeliveryDate(
                    item.fplannedDeliveryDate() == null
                            ? contractEntry.getFplannedDeliveryDate()
                            : item.fplannedDeliveryDate());
            orderEntry.setFprojectId(contractEntry.getFprojectId());
            orderEntry.setFcostCenterId(contractEntry.getFcostCenterId());
            orderEntry.setFreceiptReservedQuantity(BigDecimal.ZERO);
            orderEntry.setFreceivedQuantity(BigDecimal.ZERO);
            orderEntry.setFacceptedQuantity(BigDecimal.ZERO);
            orderEntry.setFinboundQuantity(BigDecimal.ZERO);
            orderEntry.setFinvoicedQuantity(BigDecimal.ZERO);
            orderEntry.setFsettledAmount(BigDecimal.ZERO);
            orderEntry.setFcreateBy(operatorId);
            orderEntry.setFcreateTime(now);
            orderEntry.setFmodifyBy(operatorId);
            orderEntry.setFmodifyTime(now);
            orderEntry.setFdeleteFlag(0);
            orderEntry.setFversion(0);
            orderEntries.add(orderEntry);

            contractEntry.setForderedQuantity(
                    nz(contractEntry.getForderedQuantity()).add(quantity));
            touch(contractEntry, operatorId);
            requireOne(
                    contractEntryMapper.updateById(contractEntry),
                    "采购合同分录");

            requestEntry.setForderedQuantity(
                    nz(requestEntry.getForderedQuantity()).add(quantity));
            touch(requestEntry, operatorId);
            requireOne(
                    requestEntryMapper.updateById(requestEntry),
                    "采购申请分录");
            affectedRequestIds.add(requestEntry.getFpurchaseRequestId());

            totalQuantity = totalQuantity.add(quantity);
            netTotal = netTotal.add(net);
            taxTotal = taxTotal.add(tax);
            grossTotal = grossTotal.add(gross);
        }

        PurchaseOrderEntity order = new PurchaseOrderEntity();
        order.setFid(orderId);
        order.setFtenantId(tenantId);
        order.setForgId(contract.getForgId());
        order.setFnumber(number);
        order.setFdate(orderDate);
        order.setFbusinessPartnerId(contract.getFbusinessPartnerId());
        order.setFbusinessPartnerCode(contract.getFbusinessPartnerCode());
        order.setFbusinessPartnerName(contract.getFbusinessPartnerName());
        order.setFcontractId(contract.getFid());
        order.setFcurrencyId(null);
        order.setFcurrencyCode(contract.getFcurrencyCode());
        order.setFtotalQuantity(totalQuantity);
        order.setFnetAmount(money(netTotal));
        order.setFtaxAmount(money(taxTotal));
        order.setFgrossAmount(money(grossTotal));
        order.setFpaymentTermCode(contract.getFpaymentTermCode());
        order.setFplannedDeliveryDate(earliestDelivery(orderEntries));
        order.setFstatus("DRAFT");
        order.setFapprovalStatus("DRAFT");
        order.setFreceiptStatus("NONE");
        order.setFinvoiceStatus("NONE");
        order.setFsettlementStatus("NONE");
        order.setFcloseStatus("OPEN");
        order.setFcreateBy(operatorId);
        order.setFcreateTime(now);
        order.setFmodifyBy(operatorId);
        order.setFmodifyTime(now);
        order.setFdeleteFlag(0);
        order.setFversion(0);

        requireOne(orderMapper.insert(order), "采购订单");
        for (PurchaseOrderEntryEntity entry : orderEntries) {
            requireOne(orderEntryMapper.insert(entry), "采购订单分录");
        }

        refreshContractExecution(contract.getFid(), tenantId, operatorId);
        for (Long requestId : affectedRequestIds.stream().sorted().toList()) {
            refreshRequestExecution(requestId, tenantId, operatorId);
        }

        return new PurchaseOrderDetail(order, List.copyOf(orderEntries));
    }

    @Transactional(rollbackFor = Exception.class)
    public void releaseOrderAllocation(
            PurchaseOrderEntity order,
            List<PurchaseOrderEntryEntity> entries,
            Long operatorId
    ) {
        if (order == null || order.getFcontractId() == null) {
            return;
        }

        List<PurchaseOrderEntryEntity> sourcedEntries = entries == null
                ? List.of()
                : entries.stream()
                .filter(item -> item.getFcontractEntryId() != null)
                .sorted(Comparator.comparing(
                        PurchaseOrderEntryEntity::getFcontractEntryId))
                .toList();
        if (sourcedEntries.isEmpty()) {
            return;
        }

        PurchaseContractEntity contract =
                contractMapper.selectByIdForUpdate(
                        order.getFcontractId(), order.getFtenantId());
        if (contract == null) {
            throw new BizException("采购订单来源合同不存在: " + order.getFcontractId());
        }

        Set<Long> affectedRequestIds = new LinkedHashSet<>();
        for (PurchaseOrderEntryEntity orderEntry : sourcedEntries) {
            PurchaseContractEntryEntity contractEntry =
                    contractEntryMapper.selectByIdForUpdate(
                            orderEntry.getFcontractEntryId(), order.getFtenantId());
            if (contractEntry == null
                    || !contract.getFid().equals(contractEntry.getFpurchaseContractId())) {
                throw new BizException("采购订单来源合同分录不存在: "
                        + orderEntry.getFcontractEntryId());
            }

            BigDecimal newContractOrdered =
                    nz(contractEntry.getForderedQuantity())
                            .subtract(nz(orderEntry.getFquantity()));
            if (newContractOrdered.compareTo(BigDecimal.ZERO) < 0) {
                throw new BizException("合同分录已下单数量出现负数，拒绝释放: "
                        + contractEntry.getFid());
            }
            contractEntry.setForderedQuantity(newContractOrdered);
            touch(contractEntry, operatorId);
            requireOne(
                    contractEntryMapper.updateById(contractEntry),
                    "采购合同分录");

            PurchaseRequestEntryEntity requestEntry =
                    requestEntryMapper.selectByIdForUpdate(
                            orderEntry.getFpurchaseRequestEntryId(),
                            order.getFtenantId());
            if (requestEntry == null) {
                throw new BizException("采购订单来源申请分录不存在: "
                        + orderEntry.getFpurchaseRequestEntryId());
            }
            BigDecimal newRequestOrdered =
                    nz(requestEntry.getForderedQuantity())
                            .subtract(nz(orderEntry.getFquantity()));
            if (newRequestOrdered.compareTo(BigDecimal.ZERO) < 0) {
                throw new BizException("采购申请分录已下单数量出现负数，拒绝释放: "
                        + requestEntry.getFid());
            }
            requestEntry.setForderedQuantity(newRequestOrdered);
            touch(requestEntry, operatorId);
            requireOne(
                    requestEntryMapper.updateById(requestEntry),
                    "采购申请分录");
            affectedRequestIds.add(requestEntry.getFpurchaseRequestId());
        }

        refreshContractExecution(contract.getFid(), order.getFtenantId(), operatorId);
        for (Long requestId : affectedRequestIds.stream().sorted().toList()) {
            refreshRequestExecution(
                    requestId, order.getFtenantId(), operatorId);
        }
    }

    public boolean isContractSourced(List<PurchaseOrderEntryEntity> entries) {
        return entries != null && entries.stream()
                .anyMatch(item -> item.getFcontractEntryId() != null);
    }

    private void refreshContractExecution(
            Long contractId, String tenantId, Long operatorId
    ) {
        PurchaseContractEntity contract =
                contractMapper.selectByIdForUpdate(contractId, tenantId);
        if (contract == null) {
            throw new BizException("采购合同不存在: " + contractId);
        }
        List<PurchaseContractEntryEntity> entries =
                contractEntryMapper.selectList(
                        new LambdaQueryWrapper<PurchaseContractEntryEntity>()
                                .eq(PurchaseContractEntryEntity::getFpurchaseContractId,
                                        contractId)
                                .orderByAsc(PurchaseContractEntryEntity::getFlineNo));
        boolean anyOrdered = entries.stream().anyMatch(
                entry -> nz(entry.getForderedQuantity())
                        .compareTo(BigDecimal.ZERO) > 0);
        boolean allOrdered = !entries.isEmpty() && entries.stream().allMatch(
                entry -> nz(entry.getForderedQuantity())
                        .compareTo(nz(entry.getFquantity())) >= 0);
        contract.setFexecutionStatus(
                allOrdered ? EXECUTION_COMPLETE
                        : anyOrdered ? EXECUTION_ORDERING
                        : EXECUTION_NONE);
        contract.setFmodifyBy(operatorId);
        contract.setFmodifyTime(LocalDateTime.now());
        requireOne(contractMapper.updateById(contract), "采购合同");
    }

    private void refreshRequestExecution(
            Long requestId, String tenantId, Long operatorId
    ) {
        PurchaseRequestEntity request =
                requestMapper.selectByIdForUpdate(requestId, tenantId);
        if (request == null) {
            throw new BizException("采购申请不存在: " + requestId);
        }
        List<PurchaseRequestEntryEntity> entries =
                requestEntryMapper.selectList(
                        new LambdaQueryWrapper<PurchaseRequestEntryEntity>()
                                .eq(PurchaseRequestEntryEntity::getFpurchaseRequestId,
                                        requestId)
                                .orderByAsc(PurchaseRequestEntryEntity::getFlineNo));

        boolean anyOrdered = entries.stream().anyMatch(
                entry -> nz(entry.getForderedQuantity())
                        .compareTo(BigDecimal.ZERO) > 0);
        boolean allOrdered = !entries.isEmpty() && entries.stream().allMatch(
                entry -> nz(entry.getForderedQuantity())
                        .compareTo(nz(entry.getFquantity())) >= 0);
        boolean allSourced = !entries.isEmpty() && entries.stream().allMatch(
                entry -> nz(entry.getFsourcedQuantity())
                        .compareTo(nz(entry.getFquantity())) >= 0);

        String execution = allOrdered
                ? EXECUTION_COMPLETE
                : anyOrdered
                ? EXECUTION_ORDERING
                : allSourced
                ? EXECUTION_CONTRACTING
                : EXECUTION_SOURCING;
        request.setFexecutionStatus(execution);
        request.setFmodifyBy(operatorId);
        request.setFmodifyTime(LocalDateTime.now());
        requireOne(requestMapper.updateById(request), "采购申请");
    }

    private void validateContractDate(
            PurchaseContractEntity contract, LocalDate orderDate
    ) {
        if (contract.getFstartDate() != null
                && orderDate.isBefore(contract.getFstartDate())) {
            throw new BizException("采购订单日期早于合同开始日期");
        }
        if (contract.getFendDate() != null
                && orderDate.isAfter(contract.getFendDate())) {
            throw new BizException("采购订单日期晚于合同结束日期");
        }
    }

    private LocalDate earliestDelivery(
            List<PurchaseOrderEntryEntity> entries
    ) {
        return entries.stream()
                .map(PurchaseOrderEntryEntity::getFplannedDeliveryDate)
                .filter(java.util.Objects::nonNull)
                .min(LocalDate::compareTo)
                .orElse(null);
    }

    private void ensureNumberUnique(String tenantId, String number) {
        Long count = orderMapper.selectCount(
                new LambdaQueryWrapper<PurchaseOrderEntity>()
                        .eq(PurchaseOrderEntity::getFtenantId, tenantId)
                        .eq(PurchaseOrderEntity::getFnumber, number));
        if (count != null && count > 0) {
            throw new BizException("采购订单号已存在: " + number);
        }
    }

    private String buildNumber(LocalDate date, Long id) {
        String suffix = String.valueOf(id);
        suffix = suffix.substring(Math.max(0, suffix.length() - 8));
        return "PO" + date.format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + suffix;
    }

    private String requireTenant(String tenantId) {
        if (!StringUtils.hasText(tenantId)) {
            throw new BizException("tenantId 不能为空");
        }
        return tenantId.trim();
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal money(BigDecimal value) {
        return nz(value).setScale(2, RoundingMode.HALF_UP);
    }

    private void touch(
            PurchaseContractEntryEntity entry,
            Long operatorId
    ) {
        entry.setFmodifyBy(operatorId);
        entry.setFmodifyTime(LocalDateTime.now());
    }

    private void touch(
            PurchaseRequestEntryEntity entry,
            Long operatorId
    ) {
        entry.setFmodifyBy(operatorId);
        entry.setFmodifyTime(LocalDateTime.now());
    }

    private void requireOne(int affected, String objectName) {
        if (affected != 1) {
            throw new BizException(objectName + "已被其他请求修改，请刷新后重试");
        }
    }
}
