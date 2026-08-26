package single.cjj.erp.procurement.receipt.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.exception.BizException;
import single.cjj.erp.event.service.BusinessEventOutboxService;
import single.cjj.erp.procurement.order.entity.PurchaseOrderEntity;
import single.cjj.erp.procurement.order.entity.PurchaseOrderEntryEntity;
import single.cjj.erp.procurement.order.service.PurchaseOrderFulfillmentService;
import single.cjj.erp.procurement.order.service.PurchaseOrderFulfillmentService.ReservedOrderLine;
import single.cjj.erp.procurement.receipt.dto.PurchaseReceiptContracts.PurchaseReceiptCreateRequest;
import single.cjj.erp.procurement.receipt.dto.PurchaseReceiptContracts.PurchaseReceiptDetail;
import single.cjj.erp.procurement.receipt.dto.PurchaseReceiptContracts.PurchaseReceiptEntryRequest;
import single.cjj.erp.procurement.receipt.dto.PurchaseReceiptContracts.PurchaseReceiptUpdateRequest;
import single.cjj.erp.procurement.receipt.entity.PurchaseReceiptEntity;
import single.cjj.erp.procurement.receipt.entity.PurchaseReceiptEntryEntity;
import single.cjj.erp.procurement.receipt.mapper.PurchaseReceiptEntryMapper;
import single.cjj.erp.procurement.receipt.mapper.PurchaseReceiptMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PurchaseReceiptService {

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String APPROVAL_DRAFT = "DRAFT";
    private static final String APPROVAL_SUBMITTED = "SUBMITTED";
    private static final String APPROVAL_AUDITED = "AUDITED";
    private static final String APPROVAL_REJECTED = "REJECTED";

    private final PurchaseReceiptMapper receiptMapper;
    private final PurchaseReceiptEntryMapper entryMapper;
    private final PurchaseOrderFulfillmentService fulfillmentService;
    private final BusinessEventOutboxService outboxService;

    public PurchaseReceiptService(
            PurchaseReceiptMapper receiptMapper,
            PurchaseReceiptEntryMapper entryMapper,
            PurchaseOrderFulfillmentService fulfillmentService,
            BusinessEventOutboxService outboxService
    ) {
        this.receiptMapper = receiptMapper;
        this.entryMapper = entryMapper;
        this.fulfillmentService = fulfillmentService;
        this.outboxService = outboxService;
    }

    public PurchaseReceiptDetail detail(Long fid, String tenantId) {
        PurchaseReceiptEntity receipt = requireReceipt(fid, tenantId);
        return new PurchaseReceiptDetail(receipt, listEntries(fid));
    }

    public PurchaseReceiptDetail findByIdempotencyKey(String key) {
        if (!StringUtils.hasText(key)) {
            return null;
        }
        PurchaseReceiptEntity receipt = receiptMapper.selectOne(new LambdaQueryWrapper<PurchaseReceiptEntity>()
                .eq(PurchaseReceiptEntity::getFbotpIdempotencyKey, key)
                .last("limit 1"));
        return receipt == null ? null : new PurchaseReceiptDetail(receipt, listEntries(receipt.getFid()));
    }

    public IPage<PurchaseReceiptEntity> page(
            String tenantId,
            Long orgId,
            int page,
            int size,
            String number,
            Long businessPartnerId,
            String status
    ) {
        String tenant = requireTenant(tenantId);
        return receiptMapper.selectPage(new Page<>(Math.max(page, 1), Math.max(size, 1)),
                new LambdaQueryWrapper<PurchaseReceiptEntity>()
                        .eq(PurchaseReceiptEntity::getFtenantId, tenant)
                        .eq(orgId != null, PurchaseReceiptEntity::getForgId, orgId)
                        .like(StringUtils.hasText(number), PurchaseReceiptEntity::getFnumber, number)
                        .eq(businessPartnerId != null, PurchaseReceiptEntity::getFbusinessPartnerId, businessPartnerId)
                        .eq(StringUtils.hasText(status), PurchaseReceiptEntity::getFstatus, status)
                        .orderByDesc(PurchaseReceiptEntity::getFdate)
                        .orderByDesc(PurchaseReceiptEntity::getFcreateTime));
    }

    @Transactional(rollbackFor = Exception.class)
    public PurchaseReceiptDetail create(PurchaseReceiptCreateRequest request, Long operatorId) {
        if (StringUtils.hasText(request.fbotpIdempotencyKey())) {
            PurchaseReceiptDetail existing = findByIdempotencyKey(request.fbotpIdempotencyKey());
            if (existing != null) {
                return existing;
            }
        }

        String tenantId = requireTenant(request.ftenantId());
        LocalDate date = request.fdate() == null ? LocalDate.now() : request.fdate();
        Long receiptId = IdWorker.getId();
        String number = StringUtils.hasText(request.fnumber())
                ? request.fnumber().trim()
                : buildNumber(date, receiptId);
        ensureNumberUnique(tenantId, number);

        List<PurchaseReceiptEntryEntity> entries = reserveAndBuildEntries(
                receiptId,
                tenantId,
                request.forgId(),
                request.fbusinessPartnerId(),
                request.fcurrencyCode(),
                request.fwarehouseId(),
                request.entries(),
                operatorId
        );

        PurchaseReceiptEntity receipt = new PurchaseReceiptEntity();
        receipt.setFid(receiptId);
        receipt.setFtenantId(tenantId);
        receipt.setForgId(request.forgId());
        receipt.setFnumber(number);
        receipt.setFdate(date);
        receipt.setFbusinessPartnerId(request.fbusinessPartnerId());
        receipt.setFbusinessPartnerCode(request.fbusinessPartnerCode().trim());
        receipt.setFbusinessPartnerName(request.fbusinessPartnerName().trim());
        receipt.setFcurrencyCode(request.fcurrencyCode().trim());
        receipt.setFsupplierDeliveryNo(trimToNull(request.fsupplierDeliveryNo()));
        receipt.setFwarehouseId(request.fwarehouseId());
        receipt.setFstatus(STATUS_DRAFT);
        receipt.setFapprovalStatus(APPROVAL_DRAFT);
        receipt.setFbotpIdempotencyKey(trimToNull(request.fbotpIdempotencyKey()));
        receipt.setFsourceExecutionId(trimToNull(request.fsourceExecutionId()));
        initAudit(receipt, operatorId);
        receiptMapper.insert(receipt);
        insertEntries(entries);
        return detail(receiptId, tenantId);
    }

    @Transactional(rollbackFor = Exception.class)
    public PurchaseReceiptDetail update(Long fid, PurchaseReceiptUpdateRequest request, Long operatorId) {
        String tenantId = requireTenant(request.ftenantId());
        PurchaseReceiptEntity receipt = requireReceiptForUpdate(fid, tenantId);
        ensureEditable(receipt);

        List<PurchaseReceiptEntryEntity> oldEntries = listEntries(fid);
        releaseReservations(tenantId, oldEntries, operatorId);
        entryMapper.delete(new LambdaQueryWrapper<PurchaseReceiptEntryEntity>()
                .eq(PurchaseReceiptEntryEntity::getFpurchaseReceiptId, fid));

        List<PurchaseReceiptEntryEntity> newEntries = reserveAndBuildEntries(
                fid,
                tenantId,
                request.forgId(),
                request.fbusinessPartnerId(),
                request.fcurrencyCode(),
                request.fwarehouseId(),
                request.entries(),
                operatorId
        );

        receipt.setForgId(request.forgId());
        receipt.setFbusinessPartnerId(request.fbusinessPartnerId());
        receipt.setFbusinessPartnerCode(request.fbusinessPartnerCode().trim());
        receipt.setFbusinessPartnerName(request.fbusinessPartnerName().trim());
        receipt.setFcurrencyCode(request.fcurrencyCode().trim());
        receipt.setFsupplierDeliveryNo(trimToNull(request.fsupplierDeliveryNo()));
        receipt.setFwarehouseId(request.fwarehouseId());
        touch(receipt, operatorId);
        requireUpdated(receiptMapper.updateById(receipt), "采购收货单更新");
        insertEntries(newEntries);
        return detail(fid, tenantId);
    }

    @Transactional(rollbackFor = Exception.class)
    public PurchaseReceiptDetail submit(Long fid, String tenantId, Long operatorId) {
        PurchaseReceiptEntity receipt = requireReceiptForUpdate(fid, tenantId);
        if (!STATUS_DRAFT.equals(receipt.getFstatus())
                || !(APPROVAL_DRAFT.equals(receipt.getFapprovalStatus())
                || APPROVAL_REJECTED.equals(receipt.getFapprovalStatus()))) {
            throw new BizException("仅草稿或已驳回采购收货单允许提交");
        }
        if (listEntries(fid).isEmpty()) {
            throw new BizException("采购收货单至少需要一条分录");
        }
        receipt.setFapprovalStatus(APPROVAL_SUBMITTED);
        touch(receipt, operatorId);
        requireUpdated(receiptMapper.updateById(receipt), "采购收货单提交");
        return detail(fid, tenantId);
    }

    @Transactional(rollbackFor = Exception.class)
    public PurchaseReceiptDetail confirm(Long fid, String tenantId, Long operatorId) {
        PurchaseReceiptEntity receipt = requireReceiptForUpdate(fid, tenantId);
        if (!STATUS_DRAFT.equals(receipt.getFstatus()) || !APPROVAL_SUBMITTED.equals(receipt.getFapprovalStatus())) {
            throw new BizException("仅已提交采购收货单允许确认");
        }
        List<PurchaseReceiptEntryEntity> entries = listEntries(fid);
        if (entries.isEmpty()) {
            throw new BizException("采购收货单至少需要一条分录");
        }
        for (PurchaseReceiptEntryEntity entry : entries) {
            fulfillmentService.confirmReceipt(
                    receipt.getFtenantId(), entry.getFpurchaseOrderEntryId(), entry.getFquantity(), operatorId);
        }
        receipt.setFstatus(STATUS_CONFIRMED);
        receipt.setFapprovalStatus(APPROVAL_AUDITED);
        touch(receipt, operatorId);
        requireUpdated(receiptMapper.updateById(receipt), "采购收货单确认");

        outboxService.append(
                receipt.getFtenantId(), receipt.getForgId(),
                "PURCHASE_RECEIPT_CONFIRMED", "PURCHASE_RECEIPT", receipt.getFid(),
                receipt.getFversion() == null ? 0L : receipt.getFversion().longValue(),
                "ERP_PURCHASE_RECEIPT", receipt.getFnumber(), receipt.getFdate(), operatorId,
                eventPayload(receipt, entries)
        );
        return detail(fid, tenantId);
    }

    @Transactional(rollbackFor = Exception.class)
    public PurchaseReceiptDetail reject(Long fid, String tenantId, Long operatorId) {
        PurchaseReceiptEntity receipt = requireReceiptForUpdate(fid, tenantId);
        if (!STATUS_DRAFT.equals(receipt.getFstatus()) || !APPROVAL_SUBMITTED.equals(receipt.getFapprovalStatus())) {
            throw new BizException("仅已提交采购收货单允许驳回");
        }
        receipt.setFapprovalStatus(APPROVAL_REJECTED);
        touch(receipt, operatorId);
        requireUpdated(receiptMapper.updateById(receipt), "采购收货单驳回");
        return detail(fid, tenantId);
    }

    @Transactional(rollbackFor = Exception.class)
    public PurchaseReceiptDetail cancel(Long fid, String tenantId, Long operatorId) {
        PurchaseReceiptEntity receipt = requireReceiptForUpdate(fid, tenantId);
        if (STATUS_CONFIRMED.equals(receipt.getFstatus())) {
            throw new BizException("已确认采购收货单不能直接取消，请走采购退货/冲销流程");
        }
        if (STATUS_CANCELLED.equals(receipt.getFstatus())) {
            return detail(fid, tenantId);
        }
        releaseReservations(receipt.getFtenantId(), listEntries(fid), operatorId);
        receipt.setFstatus(STATUS_CANCELLED);
        touch(receipt, operatorId);
        requireUpdated(receiptMapper.updateById(receipt), "采购收货单取消");
        return detail(fid, tenantId);
    }

    private List<PurchaseReceiptEntryEntity> reserveAndBuildEntries(
            Long receiptId,
            String tenantId,
            Long orgId,
            Long partnerId,
            String currencyCode,
            Long headerWarehouseId,
            List<PurchaseReceiptEntryRequest> requests,
            Long operatorId
    ) {
        if (requests == null || requests.isEmpty()) {
            throw new BizException("采购收货单至少需要一条分录");
        }
        List<PurchaseReceiptEntryEntity> entries = new ArrayList<>(requests.size());
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < requests.size(); i++) {
            PurchaseReceiptEntryRequest request = requests.get(i);
            ReservedOrderLine reserved = fulfillmentService.reserveReceipt(
                    tenantId, request.fpurchaseOrderEntryId(), request.fquantity(), partnerId, orgId, operatorId);
            PurchaseOrderEntity order = reserved.order();
            PurchaseOrderEntryEntity source = reserved.entry();
            if (!order.getFcurrencyCode().equalsIgnoreCase(currencyCode.trim())) {
                throw new BizException("收货单币种与采购订单币种不一致");
            }

            PurchaseReceiptEntryEntity entry = new PurchaseReceiptEntryEntity();
            entry.setFid(IdWorker.getId());
            entry.setFtenantId(tenantId);
            entry.setForgId(orgId);
            entry.setFpurchaseReceiptId(receiptId);
            entry.setFlineNo(i + 1);
            entry.setFpurchaseOrderId(source.getFpurchaseOrderId());
            entry.setFpurchaseOrderEntryId(source.getFid());
            entry.setFmaterialId(source.getFmaterialId());
            entry.setFmaterialCode(source.getFmaterialCode());
            entry.setFmaterialName(source.getFmaterialName());
            entry.setFspecification(source.getFspecification());
            entry.setFunitId(source.getFunitId());
            entry.setFquantity(request.fquantity());
            entry.setFbatchNo(trimToNull(request.fbatchNo()));
            entry.setFwarehouseId(request.fwarehouseId() == null ? headerWarehouseId : request.fwarehouseId());
            entry.setFinspectionReservedQuantity(BigDecimal.ZERO);
            entry.setFinspectedQuantity(BigDecimal.ZERO);
            entry.setFprojectId(source.getFprojectId());
            entry.setFcostCenterId(source.getFcostCenterId());
            entry.setFcreateBy(operatorId);
            entry.setFcreateTime(now);
            entry.setFmodifyBy(operatorId);
            entry.setFmodifyTime(now);
            entry.setFdeleteFlag(0);
            entry.setFversion(0);
            entries.add(entry);
        }
        return entries;
    }

    private void releaseReservations(String tenantId, List<PurchaseReceiptEntryEntity> entries, Long operatorId) {
        for (PurchaseReceiptEntryEntity entry : entries) {
            fulfillmentService.releaseReceiptReservation(
                    tenantId, entry.getFpurchaseOrderEntryId(), entry.getFquantity(), operatorId);
        }
    }

    private Map<String, Object> eventPayload(
            PurchaseReceiptEntity receipt,
            List<PurchaseReceiptEntryEntity> entries
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("receiptId", receipt.getFid());
        payload.put("receiptNo", receipt.getFnumber());
        payload.put("businessPartnerId", receipt.getFbusinessPartnerId());
        payload.put("businessPartnerCode", receipt.getFbusinessPartnerCode());
        payload.put("businessPartnerName", receipt.getFbusinessPartnerName());
        payload.put("currencyCode", receipt.getFcurrencyCode());
        payload.put("entries", entries.stream().map(entry -> {
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("receiptEntryId", entry.getFid());
            line.put("purchaseOrderId", entry.getFpurchaseOrderId());
            line.put("purchaseOrderEntryId", entry.getFpurchaseOrderEntryId());
            line.put("materialId", entry.getFmaterialId());
            line.put("quantity", entry.getFquantity());
            line.put("warehouseId", entry.getFwarehouseId());
            line.put("projectId", entry.getFprojectId());
            line.put("costCenterId", entry.getFcostCenterId());
            return line;
        }).toList());
        return payload;
    }

    private PurchaseReceiptEntity requireReceipt(Long fid, String tenantId) {
        PurchaseReceiptEntity receipt = receiptMapper.selectOne(new LambdaQueryWrapper<PurchaseReceiptEntity>()
                .eq(PurchaseReceiptEntity::getFid, fid)
                .eq(PurchaseReceiptEntity::getFtenantId, requireTenant(tenantId))
                .last("limit 1"));
        if (receipt == null) {
            throw new BizException("采购收货单不存在: " + fid);
        }
        return receipt;
    }

    private PurchaseReceiptEntity requireReceiptForUpdate(Long fid, String tenantId) {
        PurchaseReceiptEntity receipt = receiptMapper.selectByIdForUpdate(fid, requireTenant(tenantId));
        if (receipt == null) {
            throw new BizException("采购收货单不存在: " + fid);
        }
        return receipt;
    }

    private List<PurchaseReceiptEntryEntity> listEntries(Long receiptId) {
        return entryMapper.selectList(new LambdaQueryWrapper<PurchaseReceiptEntryEntity>()
                .eq(PurchaseReceiptEntryEntity::getFpurchaseReceiptId, receiptId)
                .orderByAsc(PurchaseReceiptEntryEntity::getFlineNo));
    }

    private void ensureEditable(PurchaseReceiptEntity receipt) {
        if (!STATUS_DRAFT.equals(receipt.getFstatus())
                || !(APPROVAL_DRAFT.equals(receipt.getFapprovalStatus())
                || APPROVAL_REJECTED.equals(receipt.getFapprovalStatus()))) {
            throw new BizException("仅草稿或已驳回采购收货单允许修改");
        }
    }

    private void ensureNumberUnique(String tenantId, String number) {
        Long count = receiptMapper.selectCount(new LambdaQueryWrapper<PurchaseReceiptEntity>()
                .eq(PurchaseReceiptEntity::getFtenantId, tenantId)
                .eq(PurchaseReceiptEntity::getFnumber, number));
        if (count != null && count > 0) {
            throw new BizException("采购收货单号已存在: " + number);
        }
    }

    private void insertEntries(List<PurchaseReceiptEntryEntity> entries) {
        for (PurchaseReceiptEntryEntity entry : entries) {
            entryMapper.insert(entry);
        }
    }

    private String buildNumber(LocalDate date, Long id) {
        String suffix = String.valueOf(id);
        suffix = suffix.substring(Math.max(0, suffix.length() - 8));
        return "PRC" + date.format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + suffix;
    }

    private String requireTenant(String tenantId) {
        if (!StringUtils.hasText(tenantId)) {
            throw new BizException("tenantId 不能为空");
        }
        return tenantId.trim();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void initAudit(PurchaseReceiptEntity receipt, Long operatorId) {
        LocalDateTime now = LocalDateTime.now();
        receipt.setFcreateBy(operatorId);
        receipt.setFcreateTime(now);
        receipt.setFmodifyBy(operatorId);
        receipt.setFmodifyTime(now);
        receipt.setFdeleteFlag(0);
        receipt.setFversion(0);
    }

    private void touch(PurchaseReceiptEntity receipt, Long operatorId) {
        receipt.setFmodifyBy(operatorId);
        receipt.setFmodifyTime(LocalDateTime.now());
    }

    private void requireUpdated(int updated, String action) {
        if (updated != 1) {
            throw new BizException(action + "失败，数据可能已被其他请求修改");
        }
    }
}
