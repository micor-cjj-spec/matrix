package single.cjj.erp.procurement.request.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.exception.BizException;
import single.cjj.erp.event.service.BusinessEventOutboxService;
import single.cjj.erp.procurement.request.dto.PurchaseRequestContracts.ApprovalResultRequest;
import single.cjj.erp.procurement.request.dto.PurchaseRequestContracts.CreateRequest;
import single.cjj.erp.procurement.request.dto.PurchaseRequestContracts.Detail;
import single.cjj.erp.procurement.request.dto.PurchaseRequestContracts.EntryRequest;
import single.cjj.erp.procurement.request.dto.PurchaseRequestContracts.UpdateRequest;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PurchaseRequestService {

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_EFFECTIVE = "EFFECTIVE";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String APPROVAL_DRAFT = "DRAFT";
    private static final String APPROVAL_SUBMITTED = "SUBMITTED";
    private static final String APPROVAL_APPROVED = "APPROVED";
    private static final String APPROVAL_REJECTED = "REJECTED";
    private static final String EXECUTION_NONE = "NONE";
    private static final String EVENT_APPROVED = "PURCHASE_REQUEST_APPROVED";

    private final PurchaseRequestMapper requestMapper;
    private final PurchaseRequestEntryMapper entryMapper;
    private final BusinessEventOutboxService outboxService;

    public PurchaseRequestService(
            PurchaseRequestMapper requestMapper,
            PurchaseRequestEntryMapper entryMapper,
            BusinessEventOutboxService outboxService
    ) {
        this.requestMapper = requestMapper;
        this.entryMapper = entryMapper;
        this.outboxService = outboxService;
    }

    public Detail detail(Long fid, String tenantId) {
        PurchaseRequestEntity request = requireRequest(fid, tenantId, false);
        return new Detail(request, listEntries(fid));
    }

    public IPage<PurchaseRequestEntity> page(
            String tenantId,
            Long orgId,
            int page,
            int size,
            String number,
            Long requesterId,
            String approvalStatus,
            String status
    ) {
        String tenant = requireTenant(tenantId);
        return requestMapper.selectPage(
                new Page<>(Math.max(page, 1), Math.max(size, 1)),
                new LambdaQueryWrapper<PurchaseRequestEntity>()
                        .eq(PurchaseRequestEntity::getFtenantId, tenant)
                        .eq(orgId != null, PurchaseRequestEntity::getForgId, orgId)
                        .like(StringUtils.hasText(number), PurchaseRequestEntity::getFnumber, number)
                        .eq(requesterId != null, PurchaseRequestEntity::getFrequesterId, requesterId)
                        .eq(StringUtils.hasText(approvalStatus), PurchaseRequestEntity::getFapprovalStatus, approvalStatus)
                        .eq(StringUtils.hasText(status), PurchaseRequestEntity::getFstatus, status)
                        .orderByDesc(PurchaseRequestEntity::getFdate)
                        .orderByDesc(PurchaseRequestEntity::getFcreateTime)
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public Detail create(CreateRequest request, Long operatorId) {
        String tenantId = requireTenant(request.ftenantId());
        LocalDate date = request.fdate() == null ? LocalDate.now() : request.fdate();
        Long id = IdWorker.getId();
        String number = StringUtils.hasText(request.fnumber())
                ? request.fnumber().trim()
                : buildNumber(date, id);
        ensureNumberUnique(tenantId, number);

        CalculatedEntries calculated = calculateEntries(
                id, tenantId, request.forgId(), request.frequiredDate(),
                request.fprojectId(), request.fcostCenterId(), request.entries(), operatorId);

        PurchaseRequestEntity entity = new PurchaseRequestEntity();
        entity.setFid(id);
        entity.setFtenantId(tenantId);
        entity.setForgId(request.forgId());
        entity.setFnumber(number);
        entity.setFdate(date);
        entity.setFrequesterId(request.frequesterId() == null ? operatorId : request.frequesterId());
        entity.setFrequestDepartmentId(request.frequestDepartmentId());
        entity.setFrequestType(normalizeType(request.frequestType()));
        entity.setFpurpose(trimToNull(request.fpurpose()));
        entity.setFcurrencyCode(request.fcurrencyCode().trim());
        entity.setFbudgetAmount(money(request.fbudgetAmount()));
        entity.setFtotalQuantity(calculated.totalQuantity());
        entity.setFestimatedAmount(calculated.estimatedAmount());
        entity.setFrequiredDate(request.frequiredDate());
        entity.setFprojectId(request.fprojectId());
        entity.setFcostCenterId(request.fcostCenterId());
        entity.setFsourceDocumentType(trimToNull(request.fsourceDocumentType()));
        entity.setFsourceDocumentId(trimToNull(request.fsourceDocumentId()));
        entity.setFsourceDocumentNo(trimToNull(request.fsourceDocumentNo()));
        entity.setFstatus(STATUS_DRAFT);
        entity.setFapprovalStatus(APPROVAL_DRAFT);
        entity.setFexecutionStatus(EXECUTION_NONE);
        entity.setFcreateBy(operatorId);
        entity.setFcreateTime(LocalDateTime.now());
        entity.setFmodifyBy(operatorId);
        entity.setFmodifyTime(entity.getFcreateTime());
        entity.setFdeleteFlag(0);
        entity.setFversion(0);
        requestMapper.insert(entity);
        insertEntries(calculated.entries());
        return detail(id, tenantId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Detail update(Long fid, UpdateRequest request, Long operatorId) {
        String tenantId = requireTenant(request.ftenantId());
        PurchaseRequestEntity entity = requireRequest(fid, tenantId, true);
        ensureEditable(entity);

        CalculatedEntries calculated = calculateEntries(
                fid, tenantId, request.forgId(), request.frequiredDate(),
                request.fprojectId(), request.fcostCenterId(), request.entries(), operatorId);

        entity.setForgId(request.forgId());
        entity.setFrequesterId(request.frequesterId() == null ? entity.getFrequesterId() : request.frequesterId());
        entity.setFrequestDepartmentId(request.frequestDepartmentId());
        entity.setFrequestType(normalizeType(request.frequestType()));
        entity.setFpurpose(trimToNull(request.fpurpose()));
        entity.setFcurrencyCode(request.fcurrencyCode().trim());
        entity.setFbudgetAmount(money(request.fbudgetAmount()));
        entity.setFtotalQuantity(calculated.totalQuantity());
        entity.setFestimatedAmount(calculated.estimatedAmount());
        entity.setFrequiredDate(request.frequiredDate());
        entity.setFprojectId(request.fprojectId());
        entity.setFcostCenterId(request.fcostCenterId());
        entity.setFsourceDocumentType(trimToNull(request.fsourceDocumentType()));
        entity.setFsourceDocumentId(trimToNull(request.fsourceDocumentId()));
        entity.setFsourceDocumentNo(trimToNull(request.fsourceDocumentNo()));
        entity.setFrejectReason(null);
        touch(entity, operatorId);
        requireUpdated(requestMapper.updateById(entity));

        entryMapper.delete(new LambdaQueryWrapper<PurchaseRequestEntryEntity>()
                .eq(PurchaseRequestEntryEntity::getFpurchaseRequestId, fid));
        insertEntries(calculated.entries());
        return detail(fid, tenantId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Detail submit(Long fid, String tenantId, Long operatorId) {
        PurchaseRequestEntity entity = requireRequest(fid, tenantId, true);
        if (!STATUS_DRAFT.equals(entity.getFstatus())
                || !(APPROVAL_DRAFT.equals(entity.getFapprovalStatus())
                || APPROVAL_REJECTED.equals(entity.getFapprovalStatus()))) {
            throw new BizException("仅草稿或已驳回采购申请允许提交");
        }
        if (listEntries(fid).isEmpty()) {
            throw new BizException("采购申请至少需要一条分录");
        }
        entity.setFapprovalStatus(APPROVAL_SUBMITTED);
        entity.setFrejectReason(null);
        touch(entity, operatorId);
        requireUpdated(requestMapper.updateById(entity));
        return detail(fid, tenantId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Detail applyApprovalResult(
            Long fid,
            String tenantId,
            ApprovalResultRequest result
    ) {
        PurchaseRequestEntity entity = requireRequest(fid, tenantId, true);
        if (!APPROVAL_SUBMITTED.equals(entity.getFapprovalStatus())) {
            throw new BizException("只有已提交采购申请允许回写审批结果");
        }
        String approvalResult = result.status().trim().toUpperCase();
        Long operatorId = result.operatorId();
        entity.setFworkflowInstanceId(trimToNull(result.workflowInstanceId()));

        if (APPROVAL_APPROVED.equals(approvalResult)) {
            entity.setFapprovalStatus(APPROVAL_APPROVED);
            entity.setFstatus(STATUS_EFFECTIVE);
            entity.setFrejectReason(null);
            entity.setFapprovedBy(operatorId);
            entity.setFapprovedTime(LocalDateTime.now());
            touch(entity, operatorId);
            requireUpdated(requestMapper.updateById(entity));

            List<PurchaseRequestEntryEntity> entries = listEntries(fid);
            outboxService.append(
                    entity.getFtenantId(),
                    entity.getForgId(),
                    EVENT_APPROVED,
                    "PURCHASE_REQUEST",
                    entity.getFid(),
                    entity.getFversion() == null ? 0L : entity.getFversion().longValue(),
                    "ERP_PURCHASE_REQUEST",
                    entity.getFnumber(),
                    entity.getFdate(),
                    operatorId,
                    approvedPayload(entity, entries)
            );
            return new Detail(entity, entries);
        }

        if (APPROVAL_REJECTED.equals(approvalResult)) {
            entity.setFapprovalStatus(APPROVAL_REJECTED);
            entity.setFstatus(STATUS_DRAFT);
            entity.setFrejectReason(trimToNull(result.reason()));
            touch(entity, operatorId);
            requireUpdated(requestMapper.updateById(entity));
            return detail(fid, tenantId);
        }

        throw new BizException("采购申请审批结果仅支持 APPROVED / REJECTED");
    }

    @Transactional(rollbackFor = Exception.class)
    public Detail cancel(Long fid, String tenantId, Long operatorId) {
        PurchaseRequestEntity entity = requireRequest(fid, tenantId, true);
        if (!EXECUTION_NONE.equals(entity.getFexecutionStatus())) {
            throw new BizException("已进入采购执行的申请不能直接取消");
        }
        if (APPROVAL_SUBMITTED.equals(entity.getFapprovalStatus())) {
            throw new BizException("审批中的采购申请不能直接取消，请先撤销审批流程");
        }
        if (STATUS_CANCELLED.equals(entity.getFstatus())) {
            return detail(fid, tenantId);
        }
        entity.setFstatus(STATUS_CANCELLED);
        touch(entity, operatorId);
        requireUpdated(requestMapper.updateById(entity));
        return detail(fid, tenantId);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long fid, String tenantId) {
        PurchaseRequestEntity entity = requireRequest(fid, tenantId, true);
        ensureEditable(entity);
        entryMapper.delete(new LambdaQueryWrapper<PurchaseRequestEntryEntity>()
                .eq(PurchaseRequestEntryEntity::getFpurchaseRequestId, fid));
        return requestMapper.deleteById(fid) > 0;
    }

    private PurchaseRequestEntity requireRequest(Long fid, String tenantId, boolean forUpdate) {
        String tenant = requireTenant(tenantId);
        PurchaseRequestEntity entity = forUpdate
                ? requestMapper.selectByIdForUpdate(fid, tenant)
                : requestMapper.selectOne(new LambdaQueryWrapper<PurchaseRequestEntity>()
                        .eq(PurchaseRequestEntity::getFid, fid)
                        .eq(PurchaseRequestEntity::getFtenantId, tenant)
                        .last("limit 1"));
        if (entity == null) {
            throw new BizException("采购申请不存在: " + fid);
        }
        return entity;
    }

    private List<PurchaseRequestEntryEntity> listEntries(Long requestId) {
        return entryMapper.selectList(new LambdaQueryWrapper<PurchaseRequestEntryEntity>()
                .eq(PurchaseRequestEntryEntity::getFpurchaseRequestId, requestId)
                .orderByAsc(PurchaseRequestEntryEntity::getFlineNo));
    }

    private CalculatedEntries calculateEntries(
            Long requestId,
            String tenantId,
            Long orgId,
            LocalDate headerRequiredDate,
            Long headerProjectId,
            Long headerCostCenterId,
            List<EntryRequest> requests,
            Long operatorId
    ) {
        if (requests == null || requests.isEmpty()) {
            throw new BizException("采购申请至少需要一条分录");
        }
        List<PurchaseRequestEntryEntity> entries = new ArrayList<>(requests.size());
        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal estimatedAmount = BigDecimal.ZERO;
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < requests.size(); i++) {
            EntryRequest request = requests.get(i);
            BigDecimal lineAmount = money(request.fquantity().multiply(request.festimatedUnitPrice()));

            PurchaseRequestEntryEntity entry = new PurchaseRequestEntryEntity();
            entry.setFid(IdWorker.getId());
            entry.setFtenantId(tenantId);
            entry.setForgId(orgId);
            entry.setFpurchaseRequestId(requestId);
            entry.setFlineNo(i + 1);
            entry.setFmaterialId(request.fmaterialId());
            entry.setFmaterialCode(request.fmaterialCode().trim());
            entry.setFmaterialName(request.fmaterialName().trim());
            entry.setFspecification(trimToNull(request.fspecification()));
            entry.setFunitId(request.funitId());
            entry.setFquantity(request.fquantity());
            entry.setFestimatedUnitPrice(request.festimatedUnitPrice());
            entry.setFestimatedAmount(lineAmount);
            entry.setFrequiredDate(request.frequiredDate() == null ? headerRequiredDate : request.frequiredDate());
            entry.setFprojectId(request.fprojectId() == null ? headerProjectId : request.fprojectId());
            entry.setFcostCenterId(request.fcostCenterId() == null ? headerCostCenterId : request.fcostCenterId());
            entry.setFsourcedQuantity(BigDecimal.ZERO);
            entry.setForderedQuantity(BigDecimal.ZERO);
            entry.setFcreateBy(operatorId);
            entry.setFcreateTime(now);
            entry.setFmodifyBy(operatorId);
            entry.setFmodifyTime(now);
            entry.setFdeleteFlag(0);
            entry.setFversion(0);
            entries.add(entry);

            totalQuantity = totalQuantity.add(request.fquantity());
            estimatedAmount = estimatedAmount.add(lineAmount);
        }
        return new CalculatedEntries(entries, totalQuantity, estimatedAmount);
    }

    private Map<String, Object> approvedPayload(
            PurchaseRequestEntity entity,
            List<PurchaseRequestEntryEntity> entries
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("purchaseRequestId", entity.getFid());
        payload.put("purchaseRequestNo", entity.getFnumber());
        payload.put("requesterId", entity.getFrequesterId());
        payload.put("requestDepartmentId", entity.getFrequestDepartmentId());
        payload.put("requestType", entity.getFrequestType());
        payload.put("currencyCode", entity.getFcurrencyCode());
        payload.put("budgetAmount", entity.getFbudgetAmount());
        payload.put("estimatedAmount", entity.getFestimatedAmount());
        payload.put("requiredDate", entity.getFrequiredDate());
        payload.put("projectId", entity.getFprojectId());
        payload.put("costCenterId", entity.getFcostCenterId());
        payload.put("workflowInstanceId", entity.getFworkflowInstanceId());
        payload.put("entries", entries.stream().map(entry -> Map.of(
                "entryId", entry.getFid(),
                "materialId", entry.getFmaterialId(),
                "materialCode", entry.getFmaterialCode(),
                "materialName", entry.getFmaterialName(),
                "quantity", entry.getFquantity(),
                "estimatedUnitPrice", entry.getFestimatedUnitPrice(),
                "estimatedAmount", entry.getFestimatedAmount()
        )).toList());
        return payload;
    }

    private void insertEntries(List<PurchaseRequestEntryEntity> entries) {
        for (PurchaseRequestEntryEntity entry : entries) {
            entryMapper.insert(entry);
        }
    }

    private void ensureEditable(PurchaseRequestEntity entity) {
        if (!STATUS_DRAFT.equals(entity.getFstatus())
                || !(APPROVAL_DRAFT.equals(entity.getFapprovalStatus())
                || APPROVAL_REJECTED.equals(entity.getFapprovalStatus()))) {
            throw new BizException("仅草稿或已驳回采购申请允许修改/删除");
        }
    }

    private void ensureNumberUnique(String tenantId, String number) {
        Long count = requestMapper.selectCount(new LambdaQueryWrapper<PurchaseRequestEntity>()
                .eq(PurchaseRequestEntity::getFtenantId, tenantId)
                .eq(PurchaseRequestEntity::getFnumber, number));
        if (count != null && count > 0) {
            throw new BizException("采购申请单号已存在: " + number);
        }
    }

    private String buildNumber(LocalDate date, Long id) {
        String suffix = String.valueOf(id);
        suffix = suffix.substring(Math.max(0, suffix.length() - 8));
        return "PR" + date.format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + suffix;
    }

    private String normalizeType(String value) {
        if (!StringUtils.hasText(value)) {
            return "OTHER";
        }
        String type = value.trim().toUpperCase();
        return switch (type) {
            case "PROJECT", "RND", "OPERATION", "OTHER" -> type;
            default -> throw new BizException("采购申请类型仅支持 PROJECT/RND/OPERATION/OTHER");
        };
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

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2) : value.setScale(2, RoundingMode.HALF_UP);
    }

    private void touch(PurchaseRequestEntity entity, Long operatorId) {
        entity.setFmodifyBy(operatorId);
        entity.setFmodifyTime(LocalDateTime.now());
    }

    private void requireUpdated(int updated) {
        if (updated != 1) {
            throw new BizException("采购申请已被其他请求修改，请刷新后重试");
        }
    }

    private record CalculatedEntries(
            List<PurchaseRequestEntryEntity> entries,
            BigDecimal totalQuantity,
            BigDecimal estimatedAmount
    ) {
    }
}
