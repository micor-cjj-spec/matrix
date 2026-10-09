package single.cjj.erp.procurement.invoice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.exception.BizException;
import single.cjj.erp.event.service.BusinessEventOutboxService;
import single.cjj.erp.integration.fi.FiReconciliationClient;
import single.cjj.erp.integration.fi.P2pReconciliationContracts.InboundSnapshot;
import single.cjj.erp.integration.fi.P2pReconciliationContracts.InvoiceSnapshot;
import single.cjj.erp.integration.fi.P2pReconciliationContracts.PurchaseOrderSnapshot;
import single.cjj.erp.integration.fi.P2pReconciliationContracts.ThreeWayMatchLine;
import single.cjj.erp.integration.fi.P2pReconciliationContracts.ThreeWayMatchLineResult;
import single.cjj.erp.integration.fi.P2pReconciliationContracts.ThreeWayMatchRequest;
import single.cjj.erp.integration.fi.P2pReconciliationContracts.ThreeWayMatchResponse;
import single.cjj.erp.procurement.inbound.entity.PurchaseInboundEntity;
import single.cjj.erp.procurement.inbound.entity.PurchaseInboundEntryEntity;
import single.cjj.erp.procurement.inbound.mapper.PurchaseInboundEntryMapper;
import single.cjj.erp.procurement.inbound.mapper.PurchaseInboundMapper;
import single.cjj.erp.procurement.invoice.dto.SupplierInvoiceContracts.SupplierInvoiceCreateRequest;
import single.cjj.erp.procurement.invoice.dto.SupplierInvoiceContracts.SupplierInvoiceDetail;
import single.cjj.erp.procurement.invoice.dto.SupplierInvoiceContracts.SupplierInvoiceEntryRequest;
import single.cjj.erp.procurement.invoice.dto.SupplierInvoiceContracts.SupplierInvoiceUpdateRequest;
import single.cjj.erp.procurement.invoice.entity.SupplierInvoiceEntity;
import single.cjj.erp.procurement.invoice.entity.SupplierInvoiceEntryEntity;
import single.cjj.erp.procurement.invoice.mapper.SupplierInvoiceEntryMapper;
import single.cjj.erp.procurement.invoice.mapper.SupplierInvoiceMapper;
import single.cjj.erp.procurement.order.entity.PurchaseOrderEntity;
import single.cjj.erp.procurement.order.entity.PurchaseOrderEntryEntity;
import single.cjj.erp.procurement.order.mapper.PurchaseOrderEntryMapper;
import single.cjj.erp.procurement.order.mapper.PurchaseOrderMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SupplierInvoiceService {

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_SUBMITTED = "SUBMITTED";
    private static final String STATUS_ACCOUNTING_READY = "ACCOUNTING_READY";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String APPROVAL_DRAFT = "DRAFT";
    private static final String APPROVAL_SUBMITTED = "SUBMITTED";
    private static final String APPROVAL_AUDITED = "AUDITED";
    private static final String APPROVAL_REJECTED = "REJECTED";
    private static final String MATCH_UNMATCHED = "UNMATCHED";
    private static final String MATCH_MATCHING = "MATCHING";
    private static final String MATCH_MATCHED = "MATCHED";
    private static final String ACCOUNTING_NOT_READY = "NOT_READY";
    private static final String ACCOUNTING_PENDING = "PENDING";

    private final SupplierInvoiceMapper invoiceMapper;
    private final SupplierInvoiceEntryMapper entryMapper;
    private final PurchaseOrderMapper orderMapper;
    private final PurchaseOrderEntryMapper orderEntryMapper;
    private final PurchaseInboundMapper inboundMapper;
    private final PurchaseInboundEntryMapper inboundEntryMapper;
    private final FiReconciliationClient reconciliationClient;
    private final BusinessEventOutboxService outboxService;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private final String reconciliationInternalToken;

    public SupplierInvoiceService(
            SupplierInvoiceMapper invoiceMapper,
            SupplierInvoiceEntryMapper entryMapper,
            PurchaseOrderMapper orderMapper,
            PurchaseOrderEntryMapper orderEntryMapper,
            PurchaseInboundMapper inboundMapper,
            PurchaseInboundEntryMapper inboundEntryMapper,
            FiReconciliationClient reconciliationClient,
            BusinessEventOutboxService outboxService,
            ObjectMapper objectMapper,
            PlatformTransactionManager transactionManager,
            @Value("${erp.fi-reconciliation.internal-token:change-me-before-production}") String reconciliationInternalToken
    ) {
        this.invoiceMapper = invoiceMapper;
        this.entryMapper = entryMapper;
        this.orderMapper = orderMapper;
        this.orderEntryMapper = orderEntryMapper;
        this.inboundMapper = inboundMapper;
        this.inboundEntryMapper = inboundEntryMapper;
        this.reconciliationClient = reconciliationClient;
        this.outboxService = outboxService;
        this.objectMapper = objectMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.reconciliationInternalToken = reconciliationInternalToken;
    }

    @Transactional(rollbackFor = Exception.class)
    public SupplierInvoiceDetail create(SupplierInvoiceCreateRequest request, Long operatorId) {
        String tenantId = requireTenant(request.ftenantId());
        assertInvoiceNoUnique(tenantId, request.fbusinessPartnerId(), request.finvoiceNo(), null);
        LocalDateTime now = LocalDateTime.now();
        SupplierInvoiceEntity invoice = new SupplierInvoiceEntity();
        invoice.setFid(IdWorker.getId());
        invoice.setFtenantId(tenantId);
        invoice.setForgId(request.forgId());
        invoice.setFnumber(StringUtils.hasText(request.fnumber()) ? request.fnumber().trim() : buildNumber(request.finvoiceDate(), invoice.getFid()));
        invoice.setFinvoiceNo(request.finvoiceNo().trim());
        invoice.setFinvoiceCode(trimToNull(request.finvoiceCode()));
        invoice.setFinvoiceDate(request.finvoiceDate());
        invoice.setFbusinessPartnerId(request.fbusinessPartnerId());
        invoice.setFbusinessPartnerCode(request.fbusinessPartnerCode().trim());
        invoice.setFbusinessPartnerName(request.fbusinessPartnerName().trim());
        invoice.setFcurrencyCode(request.fcurrencyCode().trim());
        invoice.setFstatus(STATUS_DRAFT);
        invoice.setFapprovalStatus(APPROVAL_DRAFT);
        invoice.setFmatchStatus(MATCH_UNMATCHED);
        invoice.setFaccountingStatus(ACCOUNTING_NOT_READY);
        invoice.setFcreateBy(operatorId);
        invoice.setFcreateTime(now);
        invoice.setFmodifyBy(operatorId);
        invoice.setFmodifyTime(now);
        invoice.setFdeleteFlag(0);
        invoice.setFversion(0);
        CalculatedEntries calculated = calculateEntries(tenantId, request.forgId(), invoice.getFid(), request.entries(), operatorId, now);
        applyTotals(invoice, calculated);
        invoiceMapper.insert(invoice);
        insertEntries(calculated.entries());
        return detail(invoice.getFid(), tenantId);
    }

    @Transactional(rollbackFor = Exception.class)
    public SupplierInvoiceDetail update(Long fid, SupplierInvoiceUpdateRequest request, Long operatorId) {
        String tenantId = requireTenant(request.ftenantId());
        SupplierInvoiceEntity invoice = mustGetForUpdate(fid, tenantId);
        if (!Set.of(STATUS_DRAFT, STATUS_REJECTED).contains(invoice.getFstatus())) {
            throw new BizException("只有草稿/驳回状态的供应商发票可修改");
        }
        assertInvoiceNoUnique(tenantId, request.fbusinessPartnerId(), request.finvoiceNo(), fid);
        invoice.setForgId(request.forgId());
        invoice.setFinvoiceNo(request.finvoiceNo().trim());
        invoice.setFinvoiceCode(trimToNull(request.finvoiceCode()));
        invoice.setFinvoiceDate(request.finvoiceDate());
        invoice.setFbusinessPartnerId(request.fbusinessPartnerId());
        invoice.setFbusinessPartnerCode(request.fbusinessPartnerCode().trim());
        invoice.setFbusinessPartnerName(request.fbusinessPartnerName().trim());
        invoice.setFcurrencyCode(request.fcurrencyCode().trim());
        resetWorkflow(invoice);
        touch(invoice, operatorId);
        entryMapper.delete(new LambdaQueryWrapper<SupplierInvoiceEntryEntity>()
                .eq(SupplierInvoiceEntryEntity::getFsupplierInvoiceId, fid)
                .eq(SupplierInvoiceEntryEntity::getFtenantId, tenantId));
        CalculatedEntries calculated = calculateEntries(tenantId, request.forgId(), fid, request.entries(), operatorId, LocalDateTime.now());
        applyTotals(invoice, calculated);
        requireUpdated(invoiceMapper.updateById(invoice));
        insertEntries(calculated.entries());
        return detail(fid, tenantId);
    }

    public SupplierInvoiceDetail detail(Long fid, String tenantId) {
        String tenant = requireTenant(tenantId);
        SupplierInvoiceEntity invoice = invoiceMapper.selectOne(new LambdaQueryWrapper<SupplierInvoiceEntity>()
                .eq(SupplierInvoiceEntity::getFid, fid)
                .eq(SupplierInvoiceEntity::getFtenantId, tenant)
                .eq(SupplierInvoiceEntity::getFdeleteFlag, 0));
        if (invoice == null) {
            throw new BizException("供应商发票不存在");
        }
        return new SupplierInvoiceDetail(invoice, listEntries(fid, tenant));
    }

    public IPage<SupplierInvoiceEntity> page(String tenantId, Long orgId, int page, int size, String number,
                                              String invoiceNo, String approvalStatus, String matchStatus) {
        LambdaQueryWrapper<SupplierInvoiceEntity> wrapper = new LambdaQueryWrapper<SupplierInvoiceEntity>()
                .eq(SupplierInvoiceEntity::getFtenantId, requireTenant(tenantId))
                .eq(SupplierInvoiceEntity::getFdeleteFlag, 0);
        if (orgId != null) wrapper.eq(SupplierInvoiceEntity::getForgId, orgId);
        if (StringUtils.hasText(number)) wrapper.like(SupplierInvoiceEntity::getFnumber, number.trim());
        if (StringUtils.hasText(invoiceNo)) wrapper.like(SupplierInvoiceEntity::getFinvoiceNo, invoiceNo.trim());
        if (StringUtils.hasText(approvalStatus)) wrapper.eq(SupplierInvoiceEntity::getFapprovalStatus, approvalStatus.trim());
        if (StringUtils.hasText(matchStatus)) wrapper.eq(SupplierInvoiceEntity::getFmatchStatus, matchStatus.trim());
        wrapper.orderByDesc(SupplierInvoiceEntity::getFinvoiceDate).orderByDesc(SupplierInvoiceEntity::getFid);
        return invoiceMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    public SupplierInvoiceDetail submit(Long fid, String tenantId, Long operatorId) {
        SupplierInvoiceEntity invoice = mustGetForUpdate(fid, requireTenant(tenantId));
        if (!Set.of(STATUS_DRAFT, STATUS_REJECTED).contains(invoice.getFstatus())) {
            throw new BizException("只有草稿/驳回状态的供应商发票可提交");
        }
        if (listEntries(fid, tenantId).isEmpty()) {
            throw new BizException("供应商发票至少需要一行分录");
        }
        invoice.setFstatus(STATUS_SUBMITTED);
        invoice.setFapprovalStatus(APPROVAL_SUBMITTED);
        invoice.setFmatchStatus(MATCH_UNMATCHED);
        invoice.setFaccountingStatus(ACCOUNTING_NOT_READY);
        touch(invoice, operatorId);
        requireUpdated(invoiceMapper.updateById(invoice));
        return detail(fid, tenantId);
    }

    public SupplierInvoiceDetail match(Long fid, String tenantId, Long operatorId) {
        String tenant = requireTenant(tenantId);
        SupplierInvoiceDetail current = detail(fid, tenant);
        SupplierInvoiceEntity invoice = current.invoice();
        if (!STATUS_SUBMITTED.equals(invoice.getFstatus()) || !APPROVAL_SUBMITTED.equals(invoice.getFapprovalStatus())) {
            throw new BizException("只有已提交且待审核的供应商发票可执行三单匹配");
        }

        ThreeWayMatchRequest snapshotRequest = buildMatchRequest(current, null);
        String requestId = buildMatchRequestId(fid, snapshotRequest);
        ThreeWayMatchRequest request = withRequestId(snapshotRequest, requestId);
        transactionTemplate.executeWithoutResult(status -> markMatching(fid, tenant, requestId, operatorId));

        ThreeWayMatchResponse response;
        try {
            response = reconciliationClient.threeWayMatch(reconciliationInternalToken, request);
        } catch (Exception exception) {
            transactionTemplate.executeWithoutResult(status -> restoreMatchFailure(fid, tenant, requestId, operatorId));
            throw new BizException("三单匹配服务调用失败: " + safeMessage(exception));
        }

        transactionTemplate.executeWithoutResult(status -> applyMatchResult(fid, tenant, response, operatorId));
        return detail(fid, tenant);
    }

    @Transactional(rollbackFor = Exception.class)
    public SupplierInvoiceDetail audit(Long fid, String tenantId, Long operatorId) {
        String tenant = requireTenant(tenantId);
        SupplierInvoiceEntity invoice = mustGetForUpdate(fid, tenant);
        if (!STATUS_SUBMITTED.equals(invoice.getFstatus()) || !APPROVAL_SUBMITTED.equals(invoice.getFapprovalStatus())) {
            throw new BizException("只有已提交状态的供应商发票可审核");
        }
        if (!MATCH_MATCHED.equals(invoice.getFmatchStatus()) || invoice.getFreconciliationBatchId() == null) {
            throw new BizException("供应商发票必须完成三单匹配且结果为 MATCHED 后才能审核");
        }

        List<SupplierInvoiceEntryEntity> invoiceEntries = listEntries(fid, tenant);
        List<Long> orderIds = invoiceEntries.stream().map(SupplierInvoiceEntryEntity::getFpurchaseOrderId)
                .filter(Objects::nonNull).distinct().sorted().toList();
        Map<Long, PurchaseOrderEntity> lockedOrders = new LinkedHashMap<>();
        for (Long orderId : orderIds) {
            PurchaseOrderEntity order = orderMapper.selectByIdForUpdate(orderId, tenant);
            if (order == null) throw new BizException("采购订单不存在或已删除: " + orderId);
            lockedOrders.put(orderId, order);
        }

        List<SupplierInvoiceEntryEntity> orderedInvoiceEntries = invoiceEntries.stream()
                .sorted(Comparator.comparing(SupplierInvoiceEntryEntity::getFpurchaseOrderEntryId))
                .toList();
        Map<Long, PurchaseOrderEntryEntity> lockedOrderEntries = new LinkedHashMap<>();
        for (SupplierInvoiceEntryEntity invoiceEntry : orderedInvoiceEntries) {
            PurchaseOrderEntryEntity poEntry = orderEntryMapper.selectByIdForUpdate(invoiceEntry.getFpurchaseOrderEntryId(), tenant);
            if (poEntry == null) throw new BizException("采购订单分录不存在: " + invoiceEntry.getFpurchaseOrderEntryId());
            lockedOrderEntries.put(poEntry.getFid(), poEntry);
            BigDecimal inbound = nvl(poEntry.getFinboundQuantity());
            BigDecimal invoiced = nvl(poEntry.getFinvoicedQuantity());
            BigDecimal after = invoiced.add(invoiceEntry.getFquantity());
            if (after.compareTo(inbound) > 0) {
                throw new BizException("三单匹配后可开票数量已变化，请重新匹配；采购订单分录=" + poEntry.getFid()
                        + "，当前可开票=" + inbound.subtract(invoiced).max(BigDecimal.ZERO));
            }
            poEntry.setFinvoicedQuantity(after);
            poEntry.setFmodifyBy(operatorId);
            poEntry.setFmodifyTime(LocalDateTime.now());
            requireUpdated(orderEntryMapper.updateById(poEntry));
        }

        for (Map.Entry<Long, PurchaseOrderEntity> item : lockedOrders.entrySet()) {
            refreshOrderInvoiceStatus(item.getValue(), tenant, operatorId);
        }

        invoice.setFstatus(STATUS_ACCOUNTING_READY);
        invoice.setFapprovalStatus(APPROVAL_AUDITED);
        invoice.setFaccountingStatus(ACCOUNTING_PENDING);
        touch(invoice, operatorId);
        requireUpdated(invoiceMapper.updateById(invoice));

        outboxService.append(
                tenant,
                invoice.getForgId(),
                "SUPPLIER_INVOICE_CONFIRMED",
                "SUPPLIER_INVOICE",
                invoice.getFid(),
                invoice.getFversion() == null ? 0L : invoice.getFversion().longValue(),
                "ERP_SUPPLIER_INVOICE",
                invoice.getFnumber(),
                invoice.getFinvoiceDate(),
                operatorId,
                buildConfirmedPayload(invoice, invoiceEntries, lockedOrderEntries)
        );
        return detail(fid, tenant);
    }

    @Transactional(rollbackFor = Exception.class)
    public SupplierInvoiceDetail reject(Long fid, String tenantId, Long operatorId) {
        SupplierInvoiceEntity invoice = mustGetForUpdate(fid, requireTenant(tenantId));
        if (!STATUS_SUBMITTED.equals(invoice.getFstatus())) {
            throw new BizException("只有已提交状态的供应商发票可驳回");
        }
        invoice.setFstatus(STATUS_REJECTED);
        invoice.setFapprovalStatus(APPROVAL_REJECTED);
        invoice.setFaccountingStatus(ACCOUNTING_NOT_READY);
        touch(invoice, operatorId);
        requireUpdated(invoiceMapper.updateById(invoice));
        return detail(fid, tenantId);
    }

    @Transactional(rollbackFor = Exception.class)
    public SupplierInvoiceDetail cancel(Long fid, String tenantId, Long operatorId) {
        SupplierInvoiceEntity invoice = mustGetForUpdate(fid, requireTenant(tenantId));
        if (!Set.of(STATUS_DRAFT, STATUS_REJECTED).contains(invoice.getFstatus())) {
            throw new BizException("只有草稿/驳回状态的供应商发票可取消");
        }
        invoice.setFstatus(STATUS_CANCELLED);
        invoice.setFaccountingStatus(ACCOUNTING_NOT_READY);
        touch(invoice, operatorId);
        requireUpdated(invoiceMapper.updateById(invoice));
        return detail(fid, tenantId);
    }

    private void markMatching(Long fid, String tenantId, String requestId, Long operatorId) {
        SupplierInvoiceEntity invoice = mustGetForUpdate(fid, tenantId);
        if (!STATUS_SUBMITTED.equals(invoice.getFstatus()) || !APPROVAL_SUBMITTED.equals(invoice.getFapprovalStatus())) {
            throw new BizException("供应商发票状态已变化，请刷新后重新匹配");
        }
        if (MATCH_MATCHING.equals(invoice.getFmatchStatus())
                && StringUtils.hasText(invoice.getFmatchRequestId())
                && !Objects.equals(invoice.getFmatchRequestId(), requestId)) {
            throw new BizException("供应商发票已有另一组三单匹配正在执行");
        }
        invoice.setFmatchStatus(MATCH_MATCHING);
        invoice.setFmatchRequestId(requestId);
        touch(invoice, operatorId);
        requireUpdated(invoiceMapper.updateById(invoice));
    }

    private void restoreMatchFailure(Long fid, String tenantId, String requestId, Long operatorId) {
        SupplierInvoiceEntity invoice = mustGetForUpdate(fid, tenantId);
        if (MATCH_MATCHING.equals(invoice.getFmatchStatus())
                && Objects.equals(invoice.getFmatchRequestId(), requestId)) {
            invoice.setFmatchStatus(MATCH_UNMATCHED);
            touch(invoice, operatorId);
            requireUpdated(invoiceMapper.updateById(invoice));
        }
    }

    private void applyMatchResult(Long fid, String tenantId, ThreeWayMatchResponse response, Long operatorId) {
        if (response == null || response.batchId() == null || !StringUtils.hasText(response.result())
                || !StringUtils.hasText(response.requestId())) {
            throw new BizException("三单匹配服务返回结果不完整");
        }
        SupplierInvoiceEntity invoice = mustGetForUpdate(fid, tenantId);
        if (!STATUS_SUBMITTED.equals(invoice.getFstatus())
                || !APPROVAL_SUBMITTED.equals(invoice.getFapprovalStatus())
                || !Objects.equals(invoice.getFmatchRequestId(), response.requestId())) {
            throw new BizException("三单匹配响应已过期，当前发票状态或匹配请求已变化");
        }
        invoice.setFmatchStatus(response.result());
        invoice.setFreconciliationBatchId(response.batchId());
        invoice.setFmatchSummaryJson(toJson(response));
        invoice.setFmatchTime(LocalDateTime.now());
        touch(invoice, operatorId);
        requireUpdated(invoiceMapper.updateById(invoice));

        Map<Long, ThreeWayMatchLineResult> resultByEntry = response.lines().stream()
                .collect(Collectors.toMap(ThreeWayMatchLineResult::invoiceEntryId, line -> line));
        for (SupplierInvoiceEntryEntity entry : listEntries(fid, tenantId)) {
            ThreeWayMatchLineResult result = resultByEntry.get(entry.getFid());
            if (result == null) continue;
            entry.setFmatchStatus(result.result());
            entry.setFreconciliationCaseId(result.caseId());
            entry.setFmatchedInboundQuantity(result.availableInboundQuantity());
            entry.setFdifferenceCodes(result.differences() == null ? null : result.differences().stream()
                    .map(d -> d.code()).distinct().collect(Collectors.joining(",")));
            entry.setFmodifyBy(operatorId);
            entry.setFmodifyTime(LocalDateTime.now());
            requireUpdated(entryMapper.updateById(entry));
        }
    }

    private ThreeWayMatchRequest withRequestId(ThreeWayMatchRequest request, String requestId) {
        return new ThreeWayMatchRequest(
                requestId,
                request.tenantId(),
                request.orgId(),
                request.invoiceId(),
                request.invoiceNo(),
                request.invoiceDate(),
                request.businessPartnerId(),
                request.currencyCode(),
                request.lines()
        );
    }

    private String buildMatchRequestId(Long invoiceId, ThreeWayMatchRequest request) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("tenantId", request.tenantId());
        snapshot.put("orgId", request.orgId());
        snapshot.put("invoiceId", request.invoiceId());
        snapshot.put("invoiceNo", request.invoiceNo());
        snapshot.put("invoiceDate", request.invoiceDate());
        snapshot.put("businessPartnerId", request.businessPartnerId());
        snapshot.put("currencyCode", request.currencyCode());
        snapshot.put("lines", request.lines());
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(toJson(snapshot).getBytes(StandardCharsets.UTF_8));
            return "P2P3WAY:" + invoiceId + ":" + HexFormat.of().formatHex(digest).substring(0, 24);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 not available", exception);
        }
    }

    private ThreeWayMatchRequest buildMatchRequest(SupplierInvoiceDetail detail, String requestId) {
        SupplierInvoiceEntity invoice = detail.invoice();
        List<ThreeWayMatchLine> lines = new ArrayList<>();
        for (SupplierInvoiceEntryEntity invoiceEntry : detail.entries()) {
            PurchaseOrderEntryEntity poEntry = orderEntryMapper.selectById(invoiceEntry.getFpurchaseOrderEntryId());
            if (poEntry == null || !invoice.getFtenantId().equals(poEntry.getFtenantId()) || poEntry.getFdeleteFlag() != 0) {
                throw new BizException("采购订单分录不存在: " + invoiceEntry.getFpurchaseOrderEntryId());
            }
            PurchaseOrderEntity order = orderMapper.selectById(poEntry.getFpurchaseOrderId());
            if (order == null || !invoice.getFtenantId().equals(order.getFtenantId()) || order.getFdeleteFlag() != 0) {
                throw new BizException("采购订单不存在: " + poEntry.getFpurchaseOrderId());
            }
            if (!"AUDITED".equals(order.getFapprovalStatus()) || !"EFFECTIVE".equals(order.getFstatus())) {
                throw new BizException("三单匹配要求采购订单已审核且生效: " + order.getFnumber());
            }
            List<InboundSnapshot> inboundSnapshots = inboundEntryMapper
                    .selectConfirmedByPurchaseOrderEntry(invoice.getFtenantId(), poEntry.getFid())
                    .stream()
                    .map(inboundEntry -> inboundSnapshot(invoice.getFtenantId(), inboundEntry))
                    .toList();
            lines.add(new ThreeWayMatchLine(
                    invoiceEntry.getFid(),
                    invoiceEntry.getFlineNo(),
                    new InvoiceSnapshot(invoiceEntry.getFmaterialId(), invoiceEntry.getFmaterialCode(), invoiceEntry.getFmaterialName(),
                            invoiceEntry.getFspecification(), invoiceEntry.getFquantity(), invoiceEntry.getFunitPrice(),
                            invoiceEntry.getFnetAmount(), invoiceEntry.getFtaxRate(), invoiceEntry.getFtaxAmount(), invoiceEntry.getFgrossAmount()),
                    new PurchaseOrderSnapshot(order.getFid(), order.getFnumber(), poEntry.getFid(), order.getFbusinessPartnerId(),
                            order.getFcurrencyCode(), poEntry.getFmaterialId(), poEntry.getFmaterialCode(), poEntry.getFmaterialName(),
                            poEntry.getFspecification(), poEntry.getFquantity(), poEntry.getFinboundQuantity(), poEntry.getFinvoicedQuantity(),
                            poEntry.getFunitPrice(), poEntry.getFtaxRate()),
                    inboundSnapshots
            ));
        }
        return new ThreeWayMatchRequest(requestId, invoice.getFtenantId(), invoice.getForgId(), invoice.getFid(),
                invoice.getFnumber(), invoice.getFinvoiceDate(), invoice.getFbusinessPartnerId(), invoice.getFcurrencyCode(), lines);
    }

    private InboundSnapshot inboundSnapshot(String tenantId, PurchaseInboundEntryEntity entry) {
        PurchaseInboundEntity inbound = inboundMapper.selectById(entry.getFpurchaseInboundId());
        if (inbound == null || !tenantId.equals(inbound.getFtenantId()) || inbound.getFdeleteFlag() != 0) {
            throw new BizException("采购入库单不存在: " + entry.getFpurchaseInboundId());
        }
        return new InboundSnapshot(inbound.getFid(), inbound.getFnumber(), entry.getFid(), inbound.getFbusinessPartnerId(),
                inbound.getFcurrencyCode(), entry.getFmaterialId(), entry.getFmaterialCode(), entry.getFmaterialName(),
                entry.getFspecification(), entry.getFquantity(), entry.getFunitPrice(), entry.getFamount(), entry.getFbatchNo());
    }

    private CalculatedEntries calculateEntries(String tenantId, Long orgId, Long invoiceId,
                                                List<SupplierInvoiceEntryRequest> requests,
                                                Long operatorId, LocalDateTime now) {
        List<SupplierInvoiceEntryEntity> entries = new ArrayList<>();
        BigDecimal net = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;
        BigDecimal gross = BigDecimal.ZERO;
        for (int i = 0; i < requests.size(); i++) {
            SupplierInvoiceEntryRequest request = requests.get(i);
            PurchaseOrderEntryEntity poEntry = orderEntryMapper.selectById(request.fpurchaseOrderEntryId());
            if (poEntry == null || !tenantId.equals(poEntry.getFtenantId()) || poEntry.getFdeleteFlag() != 0) {
                throw new BizException("采购订单分录不存在: " + request.fpurchaseOrderEntryId());
            }
            BigDecimal lineNet = request.fquantity().multiply(request.funitPrice()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal taxRate = request.ftaxRate() == null ? BigDecimal.ZERO : request.ftaxRate();
            BigDecimal lineTax = lineNet.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
            SupplierInvoiceEntryEntity entry = new SupplierInvoiceEntryEntity();
            entry.setFid(IdWorker.getId());
            entry.setFtenantId(tenantId);
            entry.setForgId(orgId);
            entry.setFsupplierInvoiceId(invoiceId);
            entry.setFlineNo(i + 1);
            entry.setFpurchaseOrderId(poEntry.getFpurchaseOrderId());
            entry.setFpurchaseOrderEntryId(poEntry.getFid());
            entry.setFmaterialId(request.fmaterialId());
            entry.setFmaterialCode(request.fmaterialCode().trim());
            entry.setFmaterialName(request.fmaterialName().trim());
            entry.setFspecification(trimToNull(request.fspecification()));
            entry.setFquantity(request.fquantity());
            entry.setFunitPrice(request.funitPrice());
            entry.setFnetAmount(lineNet);
            entry.setFtaxRate(taxRate);
            entry.setFtaxAmount(lineTax);
            entry.setFgrossAmount(lineNet.add(lineTax));
            entry.setFmatchStatus(MATCH_UNMATCHED);
            entry.setFcreateBy(operatorId);
            entry.setFcreateTime(now);
            entry.setFmodifyBy(operatorId);
            entry.setFmodifyTime(now);
            entry.setFdeleteFlag(0);
            entry.setFversion(0);
            entries.add(entry);
            net = net.add(lineNet);
            tax = tax.add(lineTax);
            gross = gross.add(lineNet.add(lineTax));
        }
        return new CalculatedEntries(entries, net, tax, gross);
    }

    private void refreshOrderInvoiceStatus(PurchaseOrderEntity order, String tenantId, Long operatorId) {
        List<PurchaseOrderEntryEntity> entries = orderEntryMapper.selectList(new LambdaQueryWrapper<PurchaseOrderEntryEntity>()
                .eq(PurchaseOrderEntryEntity::getFpurchaseOrderId, order.getFid())
                .eq(PurchaseOrderEntryEntity::getFtenantId, tenantId)
                .eq(PurchaseOrderEntryEntity::getFdeleteFlag, 0));
        boolean any = entries.stream().anyMatch(entry -> nvl(entry.getFinvoicedQuantity()).signum() > 0);
        boolean complete = !entries.isEmpty() && entries.stream()
                .allMatch(entry -> nvl(entry.getFinvoicedQuantity()).compareTo(nvl(entry.getFquantity())) >= 0);
        order.setFinvoiceStatus(!any ? "NONE" : complete ? "COMPLETE" : "PARTIAL");
        order.setFmodifyBy(operatorId);
        order.setFmodifyTime(LocalDateTime.now());
        requireUpdated(orderMapper.updateById(order));
    }

    private Map<String, Object> buildConfirmedPayload(
            SupplierInvoiceEntity invoice,
            List<SupplierInvoiceEntryEntity> entries,
            Map<Long, PurchaseOrderEntryEntity> purchaseOrderEntries
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("supplierInvoiceId", invoice.getFid());
        payload.put("supplierInvoiceNo", invoice.getFnumber());
        payload.put("invoiceNo", invoice.getFinvoiceNo());
        payload.put("invoiceCode", invoice.getFinvoiceCode());
        payload.put("invoiceDate", invoice.getFinvoiceDate());
        payload.put("businessPartnerId", invoice.getFbusinessPartnerId());
        payload.put("businessPartnerCode", invoice.getFbusinessPartnerCode());
        payload.put("businessPartnerName", invoice.getFbusinessPartnerName());
        payload.put("currencyCode", invoice.getFcurrencyCode());
        payload.put("netAmount", invoice.getFnetAmount());
        payload.put("taxAmount", invoice.getFtaxAmount());
        payload.put("grossAmount", invoice.getFgrossAmount());
        payload.put("reconciliationBatchId", invoice.getFreconciliationBatchId());
        payload.put("entries", entries.stream().map(entry -> {
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("supplierInvoiceEntryId", entry.getFid());
            line.put("purchaseOrderId", entry.getFpurchaseOrderId());
            line.put("purchaseOrderEntryId", entry.getFpurchaseOrderEntryId());
            line.put("materialId", entry.getFmaterialId());
            line.put("materialCode", entry.getFmaterialCode());
            line.put("materialName", entry.getFmaterialName());
            line.put("specification", entry.getFspecification());
            line.put("quantity", entry.getFquantity());
            line.put("unitPrice", entry.getFunitPrice());
            line.put("netAmount", entry.getFnetAmount());
            line.put("taxRate", entry.getFtaxRate());
            line.put("taxAmount", entry.getFtaxAmount());
            line.put("grossAmount", entry.getFgrossAmount());
            PurchaseOrderEntryEntity poEntry = purchaseOrderEntries.get(entry.getFpurchaseOrderEntryId());
            line.put("projectId", poEntry == null ? null : poEntry.getFprojectId());
            line.put("costCenterId", poEntry == null ? null : poEntry.getFcostCenterId());
            line.put("reconciliationCaseId", entry.getFreconciliationCaseId());
            return line;
        }).toList());
        return payload;
    }

    private List<SupplierInvoiceEntryEntity> listEntries(Long invoiceId, String tenantId) {
        return entryMapper.selectList(new LambdaQueryWrapper<SupplierInvoiceEntryEntity>()
                .eq(SupplierInvoiceEntryEntity::getFsupplierInvoiceId, invoiceId)
                .eq(SupplierInvoiceEntryEntity::getFtenantId, tenantId)
                .eq(SupplierInvoiceEntryEntity::getFdeleteFlag, 0)
                .orderByAsc(SupplierInvoiceEntryEntity::getFlineNo)
                .orderByAsc(SupplierInvoiceEntryEntity::getFid));
    }

    private SupplierInvoiceEntity mustGetForUpdate(Long fid, String tenantId) {
        SupplierInvoiceEntity invoice = invoiceMapper.selectByIdForUpdate(fid, tenantId);
        if (invoice == null) throw new BizException("供应商发票不存在");
        return invoice;
    }

    private void assertInvoiceNoUnique(String tenantId, Long partnerId, String invoiceNo, Long excludeId) {
        LambdaQueryWrapper<SupplierInvoiceEntity> wrapper = new LambdaQueryWrapper<SupplierInvoiceEntity>()
                .eq(SupplierInvoiceEntity::getFtenantId, tenantId)
                .eq(SupplierInvoiceEntity::getFbusinessPartnerId, partnerId)
                .eq(SupplierInvoiceEntity::getFinvoiceNo, invoiceNo.trim())
                .eq(SupplierInvoiceEntity::getFdeleteFlag, 0);
        if (excludeId != null) wrapper.ne(SupplierInvoiceEntity::getFid, excludeId);
        if (invoiceMapper.selectCount(wrapper) > 0) {
            throw new BizException("同一供应商发票号码已存在: " + invoiceNo);
        }
    }

    private void resetWorkflow(SupplierInvoiceEntity invoice) {
        invoice.setFstatus(STATUS_DRAFT);
        invoice.setFapprovalStatus(APPROVAL_DRAFT);
        invoice.setFmatchStatus(MATCH_UNMATCHED);
        invoice.setFaccountingStatus(ACCOUNTING_NOT_READY);
        invoice.setFmatchRequestId(null);
        invoice.setFreconciliationBatchId(null);
        invoice.setFmatchSummaryJson(null);
        invoice.setFmatchTime(null);
    }

    private void insertEntries(List<SupplierInvoiceEntryEntity> entries) {
        for (SupplierInvoiceEntryEntity entry : entries) {
            entryMapper.insert(entry);
        }
    }

    private void applyTotals(SupplierInvoiceEntity invoice, CalculatedEntries calculated) {
        invoice.setFnetAmount(calculated.netAmount());
        invoice.setFtaxAmount(calculated.taxAmount());
        invoice.setFgrossAmount(calculated.grossAmount());
    }

    private String buildNumber(LocalDate date, Long id) {
        String suffix = String.valueOf(id);
        suffix = suffix.substring(Math.max(0, suffix.length() - 8));
        return "SI" + date.format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + suffix;
    }

    private String requireTenant(String tenantId) {
        if (!StringUtils.hasText(tenantId)) throw new BizException("tenantId 不能为空");
        return tenantId.trim();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void touch(SupplierInvoiceEntity invoice, Long operatorId) {
        invoice.setFmodifyBy(operatorId);
        invoice.setFmodifyTime(LocalDateTime.now());
    }

    private void requireUpdated(int updated) {
        if (updated != 1) throw new BizException("数据已被其他请求修改，请刷新后重试");
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BizException("三单匹配结果序列化失败");
        }
    }

    private String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return StringUtils.hasText(message) ? message : exception.getClass().getSimpleName();
    }

    private record CalculatedEntries(List<SupplierInvoiceEntryEntity> entries, BigDecimal netAmount,
                                     BigDecimal taxAmount, BigDecimal grossAmount) {
    }
}
