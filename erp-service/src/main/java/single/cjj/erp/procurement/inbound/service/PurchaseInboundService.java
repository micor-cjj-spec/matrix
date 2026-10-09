package single.cjj.erp.procurement.inbound.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.exception.BizException;
import single.cjj.erp.event.service.BusinessEventOutboxService;
import single.cjj.erp.procurement.acceptance.entity.PurchaseAcceptanceEntity;
import single.cjj.erp.procurement.acceptance.entity.PurchaseAcceptanceEntryEntity;
import single.cjj.erp.procurement.acceptance.mapper.PurchaseAcceptanceEntryMapper;
import single.cjj.erp.procurement.acceptance.mapper.PurchaseAcceptanceMapper;
import single.cjj.erp.procurement.inbound.dto.PurchaseInboundContracts.PurchaseInboundCreateRequest;
import single.cjj.erp.procurement.inbound.dto.PurchaseInboundContracts.PurchaseInboundDetail;
import single.cjj.erp.procurement.inbound.dto.PurchaseInboundContracts.PurchaseInboundEntryRequest;
import single.cjj.erp.procurement.inbound.dto.PurchaseInboundContracts.PurchaseInboundUpdateRequest;
import single.cjj.erp.procurement.inbound.entity.PurchaseInboundEntity;
import single.cjj.erp.procurement.inbound.entity.PurchaseInboundEntryEntity;
import single.cjj.erp.procurement.inbound.mapper.PurchaseInboundEntryMapper;
import single.cjj.erp.procurement.inbound.mapper.PurchaseInboundMapper;
import single.cjj.erp.procurement.order.service.PurchaseOrderFulfillmentService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PurchaseInboundService {

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String APPROVAL_DRAFT = "DRAFT";
    private static final String APPROVAL_SUBMITTED = "SUBMITTED";
    private static final String APPROVAL_AUDITED = "AUDITED";
    private static final String APPROVAL_REJECTED = "REJECTED";

    private final PurchaseInboundMapper inboundMapper;
    private final PurchaseInboundEntryMapper entryMapper;
    private final PurchaseAcceptanceMapper acceptanceMapper;
    private final PurchaseAcceptanceEntryMapper acceptanceEntryMapper;
    private final PurchaseOrderFulfillmentService fulfillmentService;
    private final BusinessEventOutboxService outboxService;

    public PurchaseInboundService(
            PurchaseInboundMapper inboundMapper,
            PurchaseInboundEntryMapper entryMapper,
            PurchaseAcceptanceMapper acceptanceMapper,
            PurchaseAcceptanceEntryMapper acceptanceEntryMapper,
            PurchaseOrderFulfillmentService fulfillmentService,
            BusinessEventOutboxService outboxService
    ) {
        this.inboundMapper = inboundMapper;
        this.entryMapper = entryMapper;
        this.acceptanceMapper = acceptanceMapper;
        this.acceptanceEntryMapper = acceptanceEntryMapper;
        this.fulfillmentService = fulfillmentService;
        this.outboxService = outboxService;
    }

    public PurchaseInboundDetail detail(Long fid, String tenantId) {
        PurchaseInboundEntity inbound = requireInbound(fid, tenantId);
        return new PurchaseInboundDetail(inbound, listEntries(fid));
    }

    public PurchaseInboundDetail findByIdempotencyKey(String key) {
        if (!StringUtils.hasText(key)) {
            return null;
        }
        PurchaseInboundEntity inbound = inboundMapper.selectOne(new LambdaQueryWrapper<PurchaseInboundEntity>()
                .eq(PurchaseInboundEntity::getFbotpIdempotencyKey, key)
                .last("limit 1"));
        return inbound == null ? null : new PurchaseInboundDetail(inbound, listEntries(inbound.getFid()));
    }

    public IPage<PurchaseInboundEntity> page(
            String tenantId,
            Long orgId,
            int page,
            int size,
            String number,
            String status,
            String accountingStatus
    ) {
        return inboundMapper.selectPage(new Page<>(Math.max(page, 1), Math.max(size, 1)),
                new LambdaQueryWrapper<PurchaseInboundEntity>()
                        .eq(PurchaseInboundEntity::getFtenantId, requireTenant(tenantId))
                        .eq(orgId != null, PurchaseInboundEntity::getForgId, orgId)
                        .like(StringUtils.hasText(number), PurchaseInboundEntity::getFnumber, number)
                        .eq(StringUtils.hasText(status), PurchaseInboundEntity::getFstatus, status)
                        .eq(StringUtils.hasText(accountingStatus), PurchaseInboundEntity::getFaccountingStatus, accountingStatus)
                        .orderByDesc(PurchaseInboundEntity::getFdate)
                        .orderByDesc(PurchaseInboundEntity::getFcreateTime));
    }

    @Transactional(rollbackFor = Exception.class)
    public PurchaseInboundDetail create(PurchaseInboundCreateRequest request, Long operatorId) {
        if (StringUtils.hasText(request.fbotpIdempotencyKey())) {
            PurchaseInboundDetail existing = findByIdempotencyKey(request.fbotpIdempotencyKey());
            if (existing != null) {
                return existing;
            }
        }

        String tenantId = requireTenant(request.ftenantId());
        PurchaseAcceptanceEntity acceptance = requireConfirmedAcceptance(request.fpurchaseAcceptanceId(), tenantId);
        validateHeaderAgainstAcceptance(request, acceptance);
        LocalDate date = request.fdate() == null ? LocalDate.now() : request.fdate();
        Long inboundId = IdWorker.getId();
        String number = StringUtils.hasText(request.fnumber())
                ? request.fnumber().trim()
                : buildNumber(date, inboundId);
        ensureNumberUnique(tenantId, number);

        CalculatedEntries calculated = reserveAndBuildEntries(
                inboundId,
                tenantId,
                request.forgId(),
                acceptance,
                request.fwarehouseId(),
                request.entries(),
                operatorId
        );

        PurchaseInboundEntity inbound = new PurchaseInboundEntity();
        inbound.setFid(inboundId);
        inbound.setFtenantId(tenantId);
        inbound.setForgId(request.forgId());
        inbound.setFnumber(number);
        inbound.setFdate(date);
        inbound.setFpurchaseAcceptanceId(acceptance.getFid());
        inbound.setFbusinessPartnerId(acceptance.getFbusinessPartnerId());
        inbound.setFbusinessPartnerCode(acceptance.getFbusinessPartnerCode());
        inbound.setFbusinessPartnerName(acceptance.getFbusinessPartnerName());
        inbound.setFcurrencyCode(acceptance.getFcurrencyCode());
        inbound.setFwarehouseId(request.fwarehouseId());
        inbound.setFtotalQuantity(calculated.totalQuantity());
        inbound.setFtotalAmount(calculated.totalAmount());
        inbound.setFstatus(STATUS_DRAFT);
        inbound.setFapprovalStatus(APPROVAL_DRAFT);
        inbound.setFaccountingStatus("NONE");
        inbound.setFbotpIdempotencyKey(trimToNull(request.fbotpIdempotencyKey()));
        inbound.setFsourceExecutionId(trimToNull(request.fsourceExecutionId()));
        initAudit(inbound, operatorId);
        inboundMapper.insert(inbound);
        insertEntries(calculated.entries());
        return detail(inboundId, tenantId);
    }

    @Transactional(rollbackFor = Exception.class)
    public PurchaseInboundDetail update(Long fid, PurchaseInboundUpdateRequest request, Long operatorId) {
        String tenantId = requireTenant(request.ftenantId());
        PurchaseInboundEntity inbound = requireInboundForUpdate(fid, tenantId);
        ensureEditable(inbound);
        if (!inbound.getForgId().equals(request.forgId())) {
            throw new BizException("采购入库单不允许变更组织");
        }
        PurchaseAcceptanceEntity acceptance = requireConfirmedAcceptance(inbound.getFpurchaseAcceptanceId(), tenantId);

        List<PurchaseInboundEntryEntity> oldEntries = listEntries(fid);
        releaseInboundReservations(tenantId, oldEntries, operatorId);
        entryMapper.delete(new LambdaQueryWrapper<PurchaseInboundEntryEntity>()
                .eq(PurchaseInboundEntryEntity::getFpurchaseInboundId, fid));

        CalculatedEntries calculated = reserveAndBuildEntries(
                fid,
                tenantId,
                inbound.getForgId(),
                acceptance,
                request.fwarehouseId(),
                request.entries(),
                operatorId
        );
        inbound.setFwarehouseId(request.fwarehouseId());
        inbound.setFtotalQuantity(calculated.totalQuantity());
        inbound.setFtotalAmount(calculated.totalAmount());
        touch(inbound, operatorId);
        requireUpdated(inboundMapper.updateById(inbound), "采购入库单更新");
        insertEntries(calculated.entries());
        return detail(fid, tenantId);
    }

    @Transactional(rollbackFor = Exception.class)
    public PurchaseInboundDetail submit(Long fid, String tenantId, Long operatorId) {
        PurchaseInboundEntity inbound = requireInboundForUpdate(fid, tenantId);
        if (!STATUS_DRAFT.equals(inbound.getFstatus())
                || !(APPROVAL_DRAFT.equals(inbound.getFapprovalStatus())
                || APPROVAL_REJECTED.equals(inbound.getFapprovalStatus()))) {
            throw new BizException("仅草稿或已驳回采购入库单允许提交");
        }
        if (listEntries(fid).isEmpty()) {
            throw new BizException("采购入库单至少需要一条分录");
        }
        inbound.setFapprovalStatus(APPROVAL_SUBMITTED);
        touch(inbound, operatorId);
        requireUpdated(inboundMapper.updateById(inbound), "采购入库单提交");
        return detail(fid, tenantId);
    }

    @Transactional(rollbackFor = Exception.class)
    public PurchaseInboundDetail confirm(Long fid, String tenantId, Long operatorId) {
        PurchaseInboundEntity inbound = requireInboundForUpdate(fid, tenantId);
        if (!STATUS_DRAFT.equals(inbound.getFstatus())
                || !APPROVAL_SUBMITTED.equals(inbound.getFapprovalStatus())) {
            throw new BizException("仅已提交采购入库单允许确认");
        }
        List<PurchaseInboundEntryEntity> entries = listEntries(fid);
        if (entries.isEmpty()) {
            throw new BizException("采购入库单至少需要一条分录");
        }

        for (PurchaseInboundEntryEntity entry : entries) {
            confirmInboundAllocation(inbound.getFtenantId(), entry, operatorId);
            fulfillmentService.confirmInboundQuantity(
                    inbound.getFtenantId(), entry.getFpurchaseOrderEntryId(), entry.getFquantity(), operatorId);
        }

        inbound.setFstatus(STATUS_CONFIRMED);
        inbound.setFapprovalStatus(APPROVAL_AUDITED);
        inbound.setFaccountingStatus("PENDING");
        touch(inbound, operatorId);
        requireUpdated(inboundMapper.updateById(inbound), "采购入库单确认");

        outboxService.append(
                inbound.getFtenantId(), inbound.getForgId(),
                "PURCHASE_INBOUND_CONFIRMED", "PURCHASE_INBOUND", inbound.getFid(),
                inbound.getFversion() == null ? 0L : inbound.getFversion().longValue(),
                "ERP_PURCHASE_INBOUND", inbound.getFnumber(), inbound.getFdate(), operatorId,
                eventPayload(inbound, entries)
        );
        return detail(fid, tenantId);
    }

    @Transactional(rollbackFor = Exception.class)
    public PurchaseInboundDetail reject(Long fid, String tenantId, Long operatorId) {
        PurchaseInboundEntity inbound = requireInboundForUpdate(fid, tenantId);
        if (!STATUS_DRAFT.equals(inbound.getFstatus())
                || !APPROVAL_SUBMITTED.equals(inbound.getFapprovalStatus())) {
            throw new BizException("仅已提交采购入库单允许驳回");
        }
        inbound.setFapprovalStatus(APPROVAL_REJECTED);
        touch(inbound, operatorId);
        requireUpdated(inboundMapper.updateById(inbound), "采购入库单驳回");
        return detail(fid, tenantId);
    }

    @Transactional(rollbackFor = Exception.class)
    public PurchaseInboundDetail cancel(Long fid, String tenantId, Long operatorId) {
        PurchaseInboundEntity inbound = requireInboundForUpdate(fid, tenantId);
        if (STATUS_CONFIRMED.equals(inbound.getFstatus())) {
            throw new BizException("已确认采购入库单不能直接取消，请走退库/冲销流程");
        }
        if (STATUS_CANCELLED.equals(inbound.getFstatus())) {
            return detail(fid, tenantId);
        }
        releaseInboundReservations(inbound.getFtenantId(), listEntries(fid), operatorId);
        inbound.setFstatus(STATUS_CANCELLED);
        touch(inbound, operatorId);
        requireUpdated(inboundMapper.updateById(inbound), "采购入库单取消");
        return detail(fid, tenantId);
    }

    private CalculatedEntries reserveAndBuildEntries(
            Long inboundId,
            String tenantId,
            Long orgId,
            PurchaseAcceptanceEntity acceptance,
            Long headerWarehouseId,
            List<PurchaseInboundEntryRequest> requests,
            Long operatorId
    ) {
        if (requests == null || requests.isEmpty()) {
            throw new BizException("采购入库单至少需要一条分录");
        }
        List<PurchaseInboundEntryEntity> entries = new ArrayList<>(requests.size());
        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < requests.size(); i++) {
            PurchaseInboundEntryRequest request = requests.get(i);
            PurchaseAcceptanceEntryEntity source = acceptanceEntryMapper.selectByIdForUpdate(
                    request.fpurchaseAcceptanceEntryId(), tenantId);
            if (source == null) {
                throw new BizException("采购验收分录不存在: " + request.fpurchaseAcceptanceEntryId());
            }
            if (!acceptance.getFid().equals(source.getFpurchaseAcceptanceId())) {
                throw new BizException("采购入库分录必须来源于同一采购验收单");
            }
            BigDecimal quantity = request.fquantity();
            requirePositive(quantity, "入库数量");
            BigDecimal accepted = nz(source.getFqualifiedQuantity()).add(nz(source.getFconcessionQuantity()));
            BigDecimal available = accepted
                    .subtract(nz(source.getFinboundQuantity()))
                    .subtract(nz(source.getFinboundReservedQuantity()));
            if (quantity.compareTo(available) > 0) {
                throw new BizException("采购验收分录可入库数量不足，当前可用: " + plain(available));
            }
            source.setFinboundReservedQuantity(nz(source.getFinboundReservedQuantity()).add(quantity));
            source.setFmodifyBy(operatorId);
            source.setFmodifyTime(now);
            requireUpdated(acceptanceEntryMapper.updateById(source), "采购入库数量预占");

            BigDecimal unitPrice = nz(source.getFunitPrice());
            BigDecimal amount = quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
            PurchaseInboundEntryEntity entry = new PurchaseInboundEntryEntity();
            entry.setFid(IdWorker.getId());
            entry.setFtenantId(tenantId);
            entry.setForgId(orgId);
            entry.setFpurchaseInboundId(inboundId);
            entry.setFlineNo(i + 1);
            entry.setFpurchaseAcceptanceId(acceptance.getFid());
            entry.setFpurchaseAcceptanceEntryId(source.getFid());
            entry.setFpurchaseReceiptEntryId(source.getFpurchaseReceiptEntryId());
            entry.setFpurchaseOrderId(source.getFpurchaseOrderId());
            entry.setFpurchaseOrderEntryId(source.getFpurchaseOrderEntryId());
            entry.setFmaterialId(source.getFmaterialId());
            entry.setFmaterialCode(source.getFmaterialCode());
            entry.setFmaterialName(source.getFmaterialName());
            entry.setFspecification(source.getFspecification());
            entry.setFunitId(source.getFunitId());
            entry.setFquantity(quantity);
            entry.setFunitPrice(unitPrice);
            entry.setFamount(amount);
            entry.setFbatchNo(StringUtils.hasText(request.fbatchNo()) ? request.fbatchNo().trim() : source.getFbatchNo());
            entry.setFwarehouseId(request.fwarehouseId() == null ? headerWarehouseId : request.fwarehouseId());
            entry.setFprojectId(source.getFprojectId());
            entry.setFcostCenterId(source.getFcostCenterId());
            entry.setFcreateBy(operatorId);
            entry.setFcreateTime(now);
            entry.setFmodifyBy(operatorId);
            entry.setFmodifyTime(now);
            entry.setFdeleteFlag(0);
            entry.setFversion(0);
            entries.add(entry);
            totalQuantity = totalQuantity.add(quantity);
            totalAmount = totalAmount.add(amount);
        }
        return new CalculatedEntries(entries, totalQuantity, totalAmount);
    }

    private void confirmInboundAllocation(
            String tenantId,
            PurchaseInboundEntryEntity inboundEntry,
            Long operatorId
    ) {
        PurchaseAcceptanceEntryEntity acceptanceEntry = acceptanceEntryMapper.selectByIdForUpdate(
                inboundEntry.getFpurchaseAcceptanceEntryId(), tenantId);
        if (acceptanceEntry == null) {
            throw new BizException("采购验收分录不存在: " + inboundEntry.getFpurchaseAcceptanceEntryId());
        }
        BigDecimal quantity = inboundEntry.getFquantity();
        BigDecimal reserved = nz(acceptanceEntry.getFinboundReservedQuantity());
        if (quantity.compareTo(reserved) > 0) {
            throw new BizException("确认入库数量超过预占数量");
        }
        BigDecimal accepted = nz(acceptanceEntry.getFqualifiedQuantity())
                .add(nz(acceptanceEntry.getFconcessionQuantity()));
        BigDecimal inbound = nz(acceptanceEntry.getFinboundQuantity()).add(quantity);
        if (inbound.compareTo(accepted) > 0) {
            throw new BizException("累计入库数量不能超过验收可入库数量");
        }
        acceptanceEntry.setFinboundReservedQuantity(reserved.subtract(quantity));
        acceptanceEntry.setFinboundQuantity(inbound);
        acceptanceEntry.setFmodifyBy(operatorId);
        acceptanceEntry.setFmodifyTime(LocalDateTime.now());
        requireUpdated(acceptanceEntryMapper.updateById(acceptanceEntry), "采购验收入库反写");
    }

    private void releaseInboundReservations(
            String tenantId,
            List<PurchaseInboundEntryEntity> entries,
            Long operatorId
    ) {
        for (PurchaseInboundEntryEntity entry : entries) {
            PurchaseAcceptanceEntryEntity source = acceptanceEntryMapper.selectByIdForUpdate(
                    entry.getFpurchaseAcceptanceEntryId(), tenantId);
            if (source == null) {
                throw new BizException("采购验收分录不存在: " + entry.getFpurchaseAcceptanceEntryId());
            }
            BigDecimal reserved = nz(source.getFinboundReservedQuantity());
            if (entry.getFquantity().compareTo(reserved) > 0) {
                throw new BizException("采购入库预占数据不一致，无法释放");
            }
            source.setFinboundReservedQuantity(reserved.subtract(entry.getFquantity()));
            source.setFmodifyBy(operatorId);
            source.setFmodifyTime(LocalDateTime.now());
            requireUpdated(acceptanceEntryMapper.updateById(source), "释放采购入库预占");
        }
    }

    private Map<String, Object> eventPayload(
            PurchaseInboundEntity inbound,
            List<PurchaseInboundEntryEntity> entries
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("inboundId", inbound.getFid());
        payload.put("inboundNo", inbound.getFnumber());
        payload.put("purchaseAcceptanceId", inbound.getFpurchaseAcceptanceId());
        payload.put("businessPartnerId", inbound.getFbusinessPartnerId());
        payload.put("businessPartnerCode", inbound.getFbusinessPartnerCode());
        payload.put("businessPartnerName", inbound.getFbusinessPartnerName());
        payload.put("currencyCode", inbound.getFcurrencyCode());
        payload.put("totalQuantity", inbound.getFtotalQuantity());
        payload.put("totalAmount", inbound.getFtotalAmount());
        payload.put("entries", entries.stream().map(entry -> {
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("inboundEntryId", entry.getFid());
            line.put("acceptanceEntryId", entry.getFpurchaseAcceptanceEntryId());
            line.put("receiptEntryId", entry.getFpurchaseReceiptEntryId());
            line.put("purchaseOrderId", entry.getFpurchaseOrderId());
            line.put("purchaseOrderEntryId", entry.getFpurchaseOrderEntryId());
            line.put("materialId", entry.getFmaterialId());
            line.put("materialCode", entry.getFmaterialCode());
            line.put("materialName", entry.getFmaterialName());
            line.put("quantity", entry.getFquantity());
            line.put("unitPrice", entry.getFunitPrice());
            line.put("amount", entry.getFamount());
            line.put("warehouseId", entry.getFwarehouseId());
            line.put("projectId", entry.getFprojectId());
            line.put("costCenterId", entry.getFcostCenterId());
            return line;
        }).toList());
        return payload;
    }

    private PurchaseAcceptanceEntity requireConfirmedAcceptance(Long acceptanceId, String tenantId) {
        PurchaseAcceptanceEntity acceptance = acceptanceMapper.selectOne(
                new LambdaQueryWrapper<PurchaseAcceptanceEntity>()
                        .eq(PurchaseAcceptanceEntity::getFid, acceptanceId)
                        .eq(PurchaseAcceptanceEntity::getFtenantId, tenantId)
                        .last("limit 1"));
        if (acceptance == null) {
            throw new BizException("采购验收单不存在: " + acceptanceId);
        }
        if (!"CONFIRMED".equals(acceptance.getFstatus()) || !"AUDITED".equals(acceptance.getFapprovalStatus())) {
            throw new BizException("仅已确认采购验收单允许生成采购入库单");
        }
        if ("REJECTED".equals(acceptance.getFresult())) {
            throw new BizException("全部不合格的采购验收单不能生成采购入库单");
        }
        return acceptance;
    }

    private void validateHeaderAgainstAcceptance(
            PurchaseInboundCreateRequest request,
            PurchaseAcceptanceEntity acceptance
    ) {
        if (!request.forgId().equals(acceptance.getForgId())) {
            throw new BizException("入库组织与采购验收单组织不一致");
        }
        if (!request.fbusinessPartnerId().equals(acceptance.getFbusinessPartnerId())) {
            throw new BizException("入库供应商与采购验收单供应商不一致");
        }
        if (!request.fcurrencyCode().trim().equalsIgnoreCase(acceptance.getFcurrencyCode())) {
            throw new BizException("入库币种与采购验收单币种不一致");
        }
    }

    private PurchaseInboundEntity requireInbound(Long fid, String tenantId) {
        PurchaseInboundEntity inbound = inboundMapper.selectOne(new LambdaQueryWrapper<PurchaseInboundEntity>()
                .eq(PurchaseInboundEntity::getFid, fid)
                .eq(PurchaseInboundEntity::getFtenantId, requireTenant(tenantId))
                .last("limit 1"));
        if (inbound == null) {
            throw new BizException("采购入库单不存在: " + fid);
        }
        return inbound;
    }

    private PurchaseInboundEntity requireInboundForUpdate(Long fid, String tenantId) {
        PurchaseInboundEntity inbound = inboundMapper.selectByIdForUpdate(fid, requireTenant(tenantId));
        if (inbound == null) {
            throw new BizException("采购入库单不存在: " + fid);
        }
        return inbound;
    }

    private List<PurchaseInboundEntryEntity> listEntries(Long inboundId) {
        return entryMapper.selectList(new LambdaQueryWrapper<PurchaseInboundEntryEntity>()
                .eq(PurchaseInboundEntryEntity::getFpurchaseInboundId, inboundId)
                .orderByAsc(PurchaseInboundEntryEntity::getFlineNo));
    }

    private void ensureEditable(PurchaseInboundEntity inbound) {
        if (!STATUS_DRAFT.equals(inbound.getFstatus())
                || !(APPROVAL_DRAFT.equals(inbound.getFapprovalStatus())
                || APPROVAL_REJECTED.equals(inbound.getFapprovalStatus()))) {
            throw new BizException("仅草稿或已驳回采购入库单允许修改");
        }
    }

    private void ensureNumberUnique(String tenantId, String number) {
        Long count = inboundMapper.selectCount(new LambdaQueryWrapper<PurchaseInboundEntity>()
                .eq(PurchaseInboundEntity::getFtenantId, tenantId)
                .eq(PurchaseInboundEntity::getFnumber, number));
        if (count != null && count > 0) {
            throw new BizException("采购入库单号已存在: " + number);
        }
    }

    private void insertEntries(List<PurchaseInboundEntryEntity> entries) {
        for (PurchaseInboundEntryEntity entry : entries) {
            entryMapper.insert(entry);
        }
    }

    private String buildNumber(LocalDate date, Long id) {
        String suffix = String.valueOf(id);
        suffix = suffix.substring(Math.max(0, suffix.length() - 8));
        return "PIN" + date.format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + suffix;
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

    private void initAudit(PurchaseInboundEntity inbound, Long operatorId) {
        LocalDateTime now = LocalDateTime.now();
        inbound.setFcreateBy(operatorId);
        inbound.setFcreateTime(now);
        inbound.setFmodifyBy(operatorId);
        inbound.setFmodifyTime(now);
        inbound.setFdeleteFlag(0);
        inbound.setFversion(0);
    }

    private void touch(PurchaseInboundEntity inbound, Long operatorId) {
        inbound.setFmodifyBy(operatorId);
        inbound.setFmodifyTime(LocalDateTime.now());
    }

    private void requirePositive(BigDecimal value, String field) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(field + "必须大于0");
        }
    }

    private void requireUpdated(int updated, String action) {
        if (updated != 1) {
            throw new BizException(action + "失败，数据可能已被其他请求修改");
        }
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String plain(BigDecimal value) {
        return nz(value).stripTrailingZeros().toPlainString();
    }

    private record CalculatedEntries(
            List<PurchaseInboundEntryEntity> entries,
            BigDecimal totalQuantity,
            BigDecimal totalAmount
    ) {
    }
}
