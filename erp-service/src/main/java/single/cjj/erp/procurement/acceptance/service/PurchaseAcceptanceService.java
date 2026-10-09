package single.cjj.erp.procurement.acceptance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.exception.BizException;
import single.cjj.erp.event.service.BusinessEventOutboxService;
import single.cjj.erp.procurement.acceptance.dto.PurchaseAcceptanceContracts.PurchaseAcceptanceCreateRequest;
import single.cjj.erp.procurement.acceptance.dto.PurchaseAcceptanceContracts.PurchaseAcceptanceDetail;
import single.cjj.erp.procurement.acceptance.dto.PurchaseAcceptanceContracts.PurchaseAcceptanceEntryRequest;
import single.cjj.erp.procurement.acceptance.dto.PurchaseAcceptanceContracts.PurchaseAcceptanceUpdateRequest;
import single.cjj.erp.procurement.acceptance.entity.PurchaseAcceptanceEntity;
import single.cjj.erp.procurement.acceptance.entity.PurchaseAcceptanceEntryEntity;
import single.cjj.erp.procurement.acceptance.mapper.PurchaseAcceptanceEntryMapper;
import single.cjj.erp.procurement.acceptance.mapper.PurchaseAcceptanceMapper;
import single.cjj.erp.procurement.order.entity.PurchaseOrderEntryEntity;
import single.cjj.erp.procurement.order.service.PurchaseOrderFulfillmentService;
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
public class PurchaseAcceptanceService {

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String APPROVAL_DRAFT = "DRAFT";
    private static final String APPROVAL_SUBMITTED = "SUBMITTED";
    private static final String APPROVAL_AUDITED = "AUDITED";
    private static final String APPROVAL_REJECTED = "REJECTED";

    private final PurchaseAcceptanceMapper acceptanceMapper;
    private final PurchaseAcceptanceEntryMapper entryMapper;
    private final PurchaseReceiptMapper receiptMapper;
    private final PurchaseReceiptEntryMapper receiptEntryMapper;
    private final PurchaseOrderFulfillmentService fulfillmentService;
    private final BusinessEventOutboxService outboxService;

    public PurchaseAcceptanceService(
            PurchaseAcceptanceMapper acceptanceMapper,
            PurchaseAcceptanceEntryMapper entryMapper,
            PurchaseReceiptMapper receiptMapper,
            PurchaseReceiptEntryMapper receiptEntryMapper,
            PurchaseOrderFulfillmentService fulfillmentService,
            BusinessEventOutboxService outboxService
    ) {
        this.acceptanceMapper = acceptanceMapper;
        this.entryMapper = entryMapper;
        this.receiptMapper = receiptMapper;
        this.receiptEntryMapper = receiptEntryMapper;
        this.fulfillmentService = fulfillmentService;
        this.outboxService = outboxService;
    }

    public PurchaseAcceptanceDetail detail(Long fid, String tenantId) {
        PurchaseAcceptanceEntity acceptance = requireAcceptance(fid, tenantId);
        return new PurchaseAcceptanceDetail(acceptance, listEntries(fid));
    }

    public PurchaseAcceptanceDetail findByIdempotencyKey(String key) {
        if (!StringUtils.hasText(key)) {
            return null;
        }
        PurchaseAcceptanceEntity acceptance = acceptanceMapper.selectOne(
                new LambdaQueryWrapper<PurchaseAcceptanceEntity>()
                        .eq(PurchaseAcceptanceEntity::getFbotpIdempotencyKey, key)
                        .last("limit 1"));
        return acceptance == null ? null : new PurchaseAcceptanceDetail(acceptance, listEntries(acceptance.getFid()));
    }

    public IPage<PurchaseAcceptanceEntity> page(
            String tenantId,
            Long orgId,
            int page,
            int size,
            String number,
            String status,
            String result
    ) {
        return acceptanceMapper.selectPage(new Page<>(Math.max(page, 1), Math.max(size, 1)),
                new LambdaQueryWrapper<PurchaseAcceptanceEntity>()
                        .eq(PurchaseAcceptanceEntity::getFtenantId, requireTenant(tenantId))
                        .eq(orgId != null, PurchaseAcceptanceEntity::getForgId, orgId)
                        .like(StringUtils.hasText(number), PurchaseAcceptanceEntity::getFnumber, number)
                        .eq(StringUtils.hasText(status), PurchaseAcceptanceEntity::getFstatus, status)
                        .eq(StringUtils.hasText(result), PurchaseAcceptanceEntity::getFresult, result)
                        .orderByDesc(PurchaseAcceptanceEntity::getFdate)
                        .orderByDesc(PurchaseAcceptanceEntity::getFcreateTime));
    }

    @Transactional(rollbackFor = Exception.class)
    public PurchaseAcceptanceDetail create(PurchaseAcceptanceCreateRequest request, Long operatorId) {
        if (StringUtils.hasText(request.fbotpIdempotencyKey())) {
            PurchaseAcceptanceDetail existing = findByIdempotencyKey(request.fbotpIdempotencyKey());
            if (existing != null) {
                return existing;
            }
        }

        String tenantId = requireTenant(request.ftenantId());
        PurchaseReceiptEntity sourceReceipt = requireConfirmedReceipt(request.fpurchaseReceiptId(), tenantId);
        validateHeaderAgainstReceipt(request.forgId(), request.fbusinessPartnerId(), request.fcurrencyCode(), sourceReceipt);

        LocalDate date = request.fdate() == null ? LocalDate.now() : request.fdate();
        Long acceptanceId = IdWorker.getId();
        String number = StringUtils.hasText(request.fnumber())
                ? request.fnumber().trim()
                : buildNumber(date, acceptanceId);
        ensureNumberUnique(tenantId, number);

        List<PurchaseAcceptanceEntryEntity> entries = reserveAndBuildEntries(
                acceptanceId,
                tenantId,
                request.forgId(),
                sourceReceipt,
                request.entries(),
                operatorId
        );

        PurchaseAcceptanceEntity acceptance = new PurchaseAcceptanceEntity();
        acceptance.setFid(acceptanceId);
        acceptance.setFtenantId(tenantId);
        acceptance.setForgId(request.forgId());
        acceptance.setFnumber(number);
        acceptance.setFdate(date);
        acceptance.setFpurchaseReceiptId(sourceReceipt.getFid());
        acceptance.setFbusinessPartnerId(sourceReceipt.getFbusinessPartnerId());
        acceptance.setFbusinessPartnerCode(sourceReceipt.getFbusinessPartnerCode());
        acceptance.setFbusinessPartnerName(sourceReceipt.getFbusinessPartnerName());
        acceptance.setFcurrencyCode(sourceReceipt.getFcurrencyCode());
        acceptance.setFstatus(STATUS_DRAFT);
        acceptance.setFapprovalStatus(APPROVAL_DRAFT);
        acceptance.setFresult("PENDING");
        acceptance.setFbotpIdempotencyKey(trimToNull(request.fbotpIdempotencyKey()));
        acceptance.setFsourceExecutionId(trimToNull(request.fsourceExecutionId()));
        initAudit(acceptance, operatorId);
        acceptanceMapper.insert(acceptance);
        insertEntries(entries);
        return detail(acceptanceId, tenantId);
    }

    @Transactional(rollbackFor = Exception.class)
    public PurchaseAcceptanceDetail update(Long fid, PurchaseAcceptanceUpdateRequest request, Long operatorId) {
        String tenantId = requireTenant(request.ftenantId());
        PurchaseAcceptanceEntity acceptance = requireAcceptanceForUpdate(fid, tenantId);
        ensureEditable(acceptance);
        if (!acceptance.getForgId().equals(request.forgId())) {
            throw new BizException("采购验收单不允许变更组织");
        }
        PurchaseReceiptEntity sourceReceipt = requireConfirmedReceipt(acceptance.getFpurchaseReceiptId(), tenantId);

        List<PurchaseAcceptanceEntryEntity> oldEntries = listEntries(fid);
        releaseInspectionReservations(tenantId, oldEntries, operatorId);
        entryMapper.delete(new LambdaQueryWrapper<PurchaseAcceptanceEntryEntity>()
                .eq(PurchaseAcceptanceEntryEntity::getFpurchaseAcceptanceId, fid));

        List<PurchaseAcceptanceEntryEntity> newEntries = reserveAndBuildEntries(
                fid, tenantId, acceptance.getForgId(), sourceReceipt, request.entries(), operatorId);
        acceptance.setFresult("PENDING");
        touch(acceptance, operatorId);
        requireUpdated(acceptanceMapper.updateById(acceptance), "采购验收单更新");
        insertEntries(newEntries);
        return detail(fid, tenantId);
    }

    @Transactional(rollbackFor = Exception.class)
    public PurchaseAcceptanceDetail submit(Long fid, String tenantId, Long operatorId) {
        PurchaseAcceptanceEntity acceptance = requireAcceptanceForUpdate(fid, tenantId);
        if (!STATUS_DRAFT.equals(acceptance.getFstatus())
                || !(APPROVAL_DRAFT.equals(acceptance.getFapprovalStatus())
                || APPROVAL_REJECTED.equals(acceptance.getFapprovalStatus()))) {
            throw new BizException("仅草稿或已驳回采购验收单允许提交");
        }
        List<PurchaseAcceptanceEntryEntity> entries = listEntries(fid);
        validateResultQuantities(entries);
        acceptance.setFapprovalStatus(APPROVAL_SUBMITTED);
        touch(acceptance, operatorId);
        requireUpdated(acceptanceMapper.updateById(acceptance), "采购验收单提交");
        return detail(fid, tenantId);
    }

    @Transactional(rollbackFor = Exception.class)
    public PurchaseAcceptanceDetail confirm(Long fid, String tenantId, Long operatorId) {
        PurchaseAcceptanceEntity acceptance = requireAcceptanceForUpdate(fid, tenantId);
        if (!STATUS_DRAFT.equals(acceptance.getFstatus())
                || !APPROVAL_SUBMITTED.equals(acceptance.getFapprovalStatus())) {
            throw new BizException("仅已提交采购验收单允许确认");
        }
        List<PurchaseAcceptanceEntryEntity> entries = listEntries(fid);
        validateResultQuantities(entries);

        BigDecimal totalAccepted = BigDecimal.ZERO;
        BigDecimal totalRejected = BigDecimal.ZERO;
        for (PurchaseAcceptanceEntryEntity entry : entries) {
            confirmInspectionAllocation(acceptance.getFtenantId(), entry, operatorId);
            BigDecimal accepted = nz(entry.getFqualifiedQuantity()).add(nz(entry.getFconcessionQuantity()));
            fulfillmentService.confirmAcceptedQuantity(
                    acceptance.getFtenantId(), entry.getFpurchaseOrderEntryId(), accepted, operatorId);
            totalAccepted = totalAccepted.add(accepted);
            totalRejected = totalRejected.add(nz(entry.getFrejectedQuantity()));
        }

        acceptance.setFresult(overallResult(totalAccepted, totalRejected));
        acceptance.setFstatus(STATUS_CONFIRMED);
        acceptance.setFapprovalStatus(APPROVAL_AUDITED);
        touch(acceptance, operatorId);
        requireUpdated(acceptanceMapper.updateById(acceptance), "采购验收单确认");

        outboxService.append(
                acceptance.getFtenantId(), acceptance.getForgId(),
                "PURCHASE_ACCEPTANCE_CONFIRMED", "PURCHASE_ACCEPTANCE", acceptance.getFid(),
                acceptance.getFversion() == null ? 0L : acceptance.getFversion().longValue(),
                "ERP_PURCHASE_ACCEPTANCE", acceptance.getFnumber(), acceptance.getFdate(), operatorId,
                eventPayload(acceptance, entries)
        );
        return detail(fid, tenantId);
    }

    @Transactional(rollbackFor = Exception.class)
    public PurchaseAcceptanceDetail reject(Long fid, String tenantId, Long operatorId) {
        PurchaseAcceptanceEntity acceptance = requireAcceptanceForUpdate(fid, tenantId);
        if (!STATUS_DRAFT.equals(acceptance.getFstatus())
                || !APPROVAL_SUBMITTED.equals(acceptance.getFapprovalStatus())) {
            throw new BizException("仅已提交采购验收单允许驳回");
        }
        acceptance.setFapprovalStatus(APPROVAL_REJECTED);
        touch(acceptance, operatorId);
        requireUpdated(acceptanceMapper.updateById(acceptance), "采购验收单驳回");
        return detail(fid, tenantId);
    }

    @Transactional(rollbackFor = Exception.class)
    public PurchaseAcceptanceDetail cancel(Long fid, String tenantId, Long operatorId) {
        PurchaseAcceptanceEntity acceptance = requireAcceptanceForUpdate(fid, tenantId);
        if (STATUS_CONFIRMED.equals(acceptance.getFstatus())) {
            throw new BizException("已确认采购验收单不能直接取消，请走后续退货/质量处理流程");
        }
        if (STATUS_CANCELLED.equals(acceptance.getFstatus())) {
            return detail(fid, tenantId);
        }
        releaseInspectionReservations(acceptance.getFtenantId(), listEntries(fid), operatorId);
        acceptance.setFstatus(STATUS_CANCELLED);
        touch(acceptance, operatorId);
        requireUpdated(acceptanceMapper.updateById(acceptance), "采购验收单取消");
        return detail(fid, tenantId);
    }

    private List<PurchaseAcceptanceEntryEntity> reserveAndBuildEntries(
            Long acceptanceId,
            String tenantId,
            Long orgId,
            PurchaseReceiptEntity sourceReceipt,
            List<PurchaseAcceptanceEntryRequest> requests,
            Long operatorId
    ) {
        if (requests == null || requests.isEmpty()) {
            throw new BizException("采购验收单至少需要一条分录");
        }
        List<PurchaseAcceptanceEntryEntity> entries = new ArrayList<>(requests.size());
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < requests.size(); i++) {
            PurchaseAcceptanceEntryRequest request = requests.get(i);
            PurchaseReceiptEntryEntity receiptEntry = receiptEntryMapper.selectByIdForUpdate(
                    request.fpurchaseReceiptEntryId(), tenantId);
            if (receiptEntry == null) {
                throw new BizException("采购收货分录不存在: " + request.fpurchaseReceiptEntryId());
            }
            if (!sourceReceipt.getFid().equals(receiptEntry.getFpurchaseReceiptId())) {
                throw new BizException("采购验收分录必须来源于同一采购收货单");
            }
            BigDecimal inspectionQuantity = request.finspectionQuantity();
            requirePositive(inspectionQuantity, "验收数量");
            BigDecimal available = nz(receiptEntry.getFquantity())
                    .subtract(nz(receiptEntry.getFinspectedQuantity()))
                    .subtract(nz(receiptEntry.getFinspectionReservedQuantity()));
            if (inspectionQuantity.compareTo(available) > 0) {
                throw new BizException("采购收货分录可验收数量不足，当前可用: " + plain(available));
            }
            receiptEntry.setFinspectionReservedQuantity(
                    nz(receiptEntry.getFinspectionReservedQuantity()).add(inspectionQuantity));
            receiptEntry.setFmodifyBy(operatorId);
            receiptEntry.setFmodifyTime(now);
            requireUpdated(receiptEntryMapper.updateById(receiptEntry), "采购验收数量预占");

            PurchaseOrderEntryEntity poEntry = fulfillmentService.requireEntry(tenantId, receiptEntry.getFpurchaseOrderEntryId());
            PurchaseAcceptanceEntryEntity entry = new PurchaseAcceptanceEntryEntity();
            entry.setFid(IdWorker.getId());
            entry.setFtenantId(tenantId);
            entry.setForgId(orgId);
            entry.setFpurchaseAcceptanceId(acceptanceId);
            entry.setFlineNo(i + 1);
            entry.setFpurchaseReceiptId(sourceReceipt.getFid());
            entry.setFpurchaseReceiptEntryId(receiptEntry.getFid());
            entry.setFpurchaseOrderId(receiptEntry.getFpurchaseOrderId());
            entry.setFpurchaseOrderEntryId(receiptEntry.getFpurchaseOrderEntryId());
            entry.setFmaterialId(receiptEntry.getFmaterialId());
            entry.setFmaterialCode(receiptEntry.getFmaterialCode());
            entry.setFmaterialName(receiptEntry.getFmaterialName());
            entry.setFspecification(receiptEntry.getFspecification());
            entry.setFunitId(receiptEntry.getFunitId());
            entry.setFinspectionQuantity(inspectionQuantity);
            entry.setFqualifiedQuantity(nz(request.fqualifiedQuantity()));
            entry.setFconcessionQuantity(nz(request.fconcessionQuantity()));
            entry.setFrejectedQuantity(nz(request.frejectedQuantity()));
            entry.setFinspectionMethod(trimToNull(request.finspectionMethod()));
            entry.setFqualityResult(lineResult(entry));
            entry.setFbatchNo(receiptEntry.getFbatchNo());
            entry.setFunitPrice(poEntry.getFunitPrice());
            entry.setFprojectId(receiptEntry.getFprojectId());
            entry.setFcostCenterId(receiptEntry.getFcostCenterId());
            entry.setFinboundReservedQuantity(BigDecimal.ZERO);
            entry.setFinboundQuantity(BigDecimal.ZERO);
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

    private void confirmInspectionAllocation(
            String tenantId,
            PurchaseAcceptanceEntryEntity acceptanceEntry,
            Long operatorId
    ) {
        PurchaseReceiptEntryEntity receiptEntry = receiptEntryMapper.selectByIdForUpdate(
                acceptanceEntry.getFpurchaseReceiptEntryId(), tenantId);
        if (receiptEntry == null) {
            throw new BizException("采购收货分录不存在: " + acceptanceEntry.getFpurchaseReceiptEntryId());
        }
        BigDecimal quantity = acceptanceEntry.getFinspectionQuantity();
        BigDecimal reserved = nz(receiptEntry.getFinspectionReservedQuantity());
        if (quantity.compareTo(reserved) > 0) {
            throw new BizException("确认验收数量超过预占数量");
        }
        BigDecimal inspected = nz(receiptEntry.getFinspectedQuantity()).add(quantity);
        if (inspected.compareTo(nz(receiptEntry.getFquantity())) > 0) {
            throw new BizException("累计验收数量不能超过收货数量");
        }
        receiptEntry.setFinspectionReservedQuantity(reserved.subtract(quantity));
        receiptEntry.setFinspectedQuantity(inspected);
        receiptEntry.setFmodifyBy(operatorId);
        receiptEntry.setFmodifyTime(LocalDateTime.now());
        requireUpdated(receiptEntryMapper.updateById(receiptEntry), "采购收货验收反写");
    }

    private void releaseInspectionReservations(
            String tenantId,
            List<PurchaseAcceptanceEntryEntity> entries,
            Long operatorId
    ) {
        for (PurchaseAcceptanceEntryEntity entry : entries) {
            PurchaseReceiptEntryEntity receiptEntry = receiptEntryMapper.selectByIdForUpdate(
                    entry.getFpurchaseReceiptEntryId(), tenantId);
            if (receiptEntry == null) {
                throw new BizException("采购收货分录不存在: " + entry.getFpurchaseReceiptEntryId());
            }
            BigDecimal reserved = nz(receiptEntry.getFinspectionReservedQuantity());
            if (entry.getFinspectionQuantity().compareTo(reserved) > 0) {
                throw new BizException("采购验收预占数据不一致，无法释放");
            }
            receiptEntry.setFinspectionReservedQuantity(reserved.subtract(entry.getFinspectionQuantity()));
            receiptEntry.setFmodifyBy(operatorId);
            receiptEntry.setFmodifyTime(LocalDateTime.now());
            requireUpdated(receiptEntryMapper.updateById(receiptEntry), "释放采购验收预占");
        }
    }

    private void validateResultQuantities(List<PurchaseAcceptanceEntryEntity> entries) {
        if (entries.isEmpty()) {
            throw new BizException("采购验收单至少需要一条分录");
        }
        for (PurchaseAcceptanceEntryEntity entry : entries) {
            BigDecimal sum = nz(entry.getFqualifiedQuantity())
                    .add(nz(entry.getFconcessionQuantity()))
                    .add(nz(entry.getFrejectedQuantity()));
            if (sum.compareTo(nz(entry.getFinspectionQuantity())) != 0) {
                throw new BizException("验收分录 " + entry.getFlineNo()
                        + " 数量不平：验收数量必须等于合格+让步接收+不合格");
            }
            entry.setFqualityResult(lineResult(entry));
        }
    }

    private String lineResult(PurchaseAcceptanceEntryEntity entry) {
        BigDecimal accepted = nz(entry.getFqualifiedQuantity()).add(nz(entry.getFconcessionQuantity()));
        BigDecimal rejected = nz(entry.getFrejectedQuantity());
        if (accepted.compareTo(BigDecimal.ZERO) > 0 && rejected.compareTo(BigDecimal.ZERO) > 0) {
            return "PARTIAL_PASSED";
        }
        if (accepted.compareTo(BigDecimal.ZERO) > 0) {
            return "PASSED";
        }
        if (rejected.compareTo(BigDecimal.ZERO) > 0) {
            return "REJECTED";
        }
        return "PENDING";
    }

    private String overallResult(BigDecimal accepted, BigDecimal rejected) {
        if (accepted.compareTo(BigDecimal.ZERO) > 0 && rejected.compareTo(BigDecimal.ZERO) > 0) {
            return "PARTIAL_PASSED";
        }
        if (accepted.compareTo(BigDecimal.ZERO) > 0) {
            return "PASSED";
        }
        return "REJECTED";
    }

    private Map<String, Object> eventPayload(
            PurchaseAcceptanceEntity acceptance,
            List<PurchaseAcceptanceEntryEntity> entries
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("acceptanceId", acceptance.getFid());
        payload.put("acceptanceNo", acceptance.getFnumber());
        payload.put("purchaseReceiptId", acceptance.getFpurchaseReceiptId());
        payload.put("businessPartnerId", acceptance.getFbusinessPartnerId());
        payload.put("result", acceptance.getFresult());
        payload.put("entries", entries.stream().map(entry -> {
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("acceptanceEntryId", entry.getFid());
            line.put("receiptEntryId", entry.getFpurchaseReceiptEntryId());
            line.put("purchaseOrderEntryId", entry.getFpurchaseOrderEntryId());
            line.put("materialId", entry.getFmaterialId());
            line.put("inspectionQuantity", entry.getFinspectionQuantity());
            line.put("qualifiedQuantity", entry.getFqualifiedQuantity());
            line.put("concessionQuantity", entry.getFconcessionQuantity());
            line.put("rejectedQuantity", entry.getFrejectedQuantity());
            line.put("qualityResult", entry.getFqualityResult());
            return line;
        }).toList());
        return payload;
    }

    private PurchaseReceiptEntity requireConfirmedReceipt(Long receiptId, String tenantId) {
        PurchaseReceiptEntity receipt = receiptMapper.selectOne(new LambdaQueryWrapper<PurchaseReceiptEntity>()
                .eq(PurchaseReceiptEntity::getFid, receiptId)
                .eq(PurchaseReceiptEntity::getFtenantId, tenantId)
                .last("limit 1"));
        if (receipt == null) {
            throw new BizException("采购收货单不存在: " + receiptId);
        }
        if (!"CONFIRMED".equals(receipt.getFstatus()) || !"AUDITED".equals(receipt.getFapprovalStatus())) {
            throw new BizException("仅已确认采购收货单允许生成验收单");
        }
        return receipt;
    }

    private void validateHeaderAgainstReceipt(
            Long orgId,
            Long partnerId,
            String currencyCode,
            PurchaseReceiptEntity receipt
    ) {
        if (!orgId.equals(receipt.getForgId())) {
            throw new BizException("验收组织与采购收货单组织不一致");
        }
        if (!partnerId.equals(receipt.getFbusinessPartnerId())) {
            throw new BizException("验收供应商与采购收货单供应商不一致");
        }
        if (!receipt.getFcurrencyCode().equalsIgnoreCase(currencyCode.trim())) {
            throw new BizException("验收币种与采购收货单币种不一致");
        }
    }

    private PurchaseAcceptanceEntity requireAcceptance(Long fid, String tenantId) {
        PurchaseAcceptanceEntity acceptance = acceptanceMapper.selectOne(
                new LambdaQueryWrapper<PurchaseAcceptanceEntity>()
                        .eq(PurchaseAcceptanceEntity::getFid, fid)
                        .eq(PurchaseAcceptanceEntity::getFtenantId, requireTenant(tenantId))
                        .last("limit 1"));
        if (acceptance == null) {
            throw new BizException("采购验收单不存在: " + fid);
        }
        return acceptance;
    }

    private PurchaseAcceptanceEntity requireAcceptanceForUpdate(Long fid, String tenantId) {
        PurchaseAcceptanceEntity acceptance = acceptanceMapper.selectByIdForUpdate(fid, requireTenant(tenantId));
        if (acceptance == null) {
            throw new BizException("采购验收单不存在: " + fid);
        }
        return acceptance;
    }

    private List<PurchaseAcceptanceEntryEntity> listEntries(Long acceptanceId) {
        return entryMapper.selectList(new LambdaQueryWrapper<PurchaseAcceptanceEntryEntity>()
                .eq(PurchaseAcceptanceEntryEntity::getFpurchaseAcceptanceId, acceptanceId)
                .orderByAsc(PurchaseAcceptanceEntryEntity::getFlineNo));
    }

    private void ensureEditable(PurchaseAcceptanceEntity acceptance) {
        if (!STATUS_DRAFT.equals(acceptance.getFstatus())
                || !(APPROVAL_DRAFT.equals(acceptance.getFapprovalStatus())
                || APPROVAL_REJECTED.equals(acceptance.getFapprovalStatus()))) {
            throw new BizException("仅草稿或已驳回采购验收单允许修改");
        }
    }

    private void ensureNumberUnique(String tenantId, String number) {
        Long count = acceptanceMapper.selectCount(new LambdaQueryWrapper<PurchaseAcceptanceEntity>()
                .eq(PurchaseAcceptanceEntity::getFtenantId, tenantId)
                .eq(PurchaseAcceptanceEntity::getFnumber, number));
        if (count != null && count > 0) {
            throw new BizException("采购验收单号已存在: " + number);
        }
    }

    private void insertEntries(List<PurchaseAcceptanceEntryEntity> entries) {
        for (PurchaseAcceptanceEntryEntity entry : entries) {
            entryMapper.insert(entry);
        }
    }

    private String buildNumber(LocalDate date, Long id) {
        String suffix = String.valueOf(id);
        suffix = suffix.substring(Math.max(0, suffix.length() - 8));
        return "PAC" + date.format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + suffix;
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

    private void initAudit(PurchaseAcceptanceEntity acceptance, Long operatorId) {
        LocalDateTime now = LocalDateTime.now();
        acceptance.setFcreateBy(operatorId);
        acceptance.setFcreateTime(now);
        acceptance.setFmodifyBy(operatorId);
        acceptance.setFmodifyTime(now);
        acceptance.setFdeleteFlag(0);
        acceptance.setFversion(0);
    }

    private void touch(PurchaseAcceptanceEntity acceptance, Long operatorId) {
        acceptance.setFmodifyBy(operatorId);
        acceptance.setFmodifyTime(LocalDateTime.now());
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
}
