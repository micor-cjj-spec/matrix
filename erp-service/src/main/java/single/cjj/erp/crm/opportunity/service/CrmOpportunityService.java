package single.cjj.erp.crm.opportunity.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.exception.BizException;
import single.cjj.erp.crm.lead.entity.CrmLeadEntity;
import single.cjj.erp.crm.lead.mapper.CrmLeadMapper;
import single.cjj.erp.crm.opportunity.dto.CrmOpportunityContracts.CreateRequest;
import single.cjj.erp.crm.opportunity.dto.CrmOpportunityContracts.UpdateRequest;
import single.cjj.erp.crm.opportunity.entity.CrmOpportunityEntity;
import single.cjj.erp.crm.opportunity.mapper.CrmOpportunityMapper;
import single.cjj.erp.crm.support.CustomerPartnerValidator;
import single.cjj.erp.event.service.BusinessEventOutboxService;
import single.cjj.erp.integration.base.BaseBusinessPartnerContracts.BusinessPartnerDetail;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Service
public class CrmOpportunityService {

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_WON = "WON";
    public static final String STATUS_LOST = "LOST";

    public static final String STAGE_DISCOVERY = "DISCOVERY";
    public static final String STAGE_QUALIFICATION = "QUALIFICATION";
    public static final String STAGE_PROPOSAL = "PROPOSAL";
    public static final String STAGE_NEGOTIATION = "NEGOTIATION";
    public static final String STAGE_WON = "WON";
    public static final String STAGE_LOST = "LOST";

    private static final Set<String> OPEN_STAGES = Set.of(
            STAGE_DISCOVERY,
            STAGE_QUALIFICATION,
            STAGE_PROPOSAL,
            STAGE_NEGOTIATION
    );

    private final CrmOpportunityMapper mapper;
    private final CrmLeadMapper leadMapper;
    private final CustomerPartnerValidator partnerValidator;
    private final BusinessEventOutboxService outboxService;

    public CrmOpportunityService(
            CrmOpportunityMapper mapper,
            CrmLeadMapper leadMapper,
            CustomerPartnerValidator partnerValidator,
            BusinessEventOutboxService outboxService
    ) {
        this.mapper = mapper;
        this.leadMapper = leadMapper;
        this.partnerValidator = partnerValidator;
        this.outboxService = outboxService;
    }

    public CrmOpportunityEntity detail(Long fid, String tenantId) {
        return requireOpportunity(fid, tenantId, false);
    }

    public IPage<CrmOpportunityEntity> page(
            String tenantId,
            Long orgId,
            int page,
            int size,
            String number,
            String name,
            Long businessPartnerId,
            Long ownerId,
            String stage,
            String status
    ) {
        String tenant = requireTenant(tenantId);
        return mapper.selectPage(
                new Page<>(Math.max(page, 1), Math.max(size, 1)),
                new LambdaQueryWrapper<CrmOpportunityEntity>()
                        .eq(CrmOpportunityEntity::getFtenantId, tenant)
                        .eq(orgId != null, CrmOpportunityEntity::getForgId, orgId)
                        .like(StringUtils.hasText(number), CrmOpportunityEntity::getFnumber, number)
                        .like(StringUtils.hasText(name), CrmOpportunityEntity::getFname, name)
                        .eq(businessPartnerId != null, CrmOpportunityEntity::getFbusinessPartnerId, businessPartnerId)
                        .eq(ownerId != null, CrmOpportunityEntity::getFownerId, ownerId)
                        .eq(StringUtils.hasText(stage), CrmOpportunityEntity::getFstage, stage)
                        .eq(StringUtils.hasText(status), CrmOpportunityEntity::getFstatus, status)
                        .orderByDesc(CrmOpportunityEntity::getFdate)
                        .orderByDesc(CrmOpportunityEntity::getFid)
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public CrmOpportunityEntity create(
            CreateRequest request,
            Long operatorId
    ) {
        String tenant = requireTenant(request.ftenantId());
        BusinessPartnerDetail partner = partnerValidator.requireActiveCustomer(
                request.fbusinessPartnerId(), tenant);

        CrmLeadEntity lead = null;
        if (request.fleadId() != null) {
            lead = leadMapper.selectByIdForUpdate(
                    request.fleadId(), tenant);
            if (lead == null) {
                throw new BizException("来源 CRM Lead 不存在: " + request.fleadId());
            }
            if (!"QUALIFIED".equals(lead.getFstatus())) {
                throw new BizException("只有 QUALIFIED Lead 允许转换为 Opportunity");
            }
        }

        LocalDate date = request.fdate() == null
                ? LocalDate.now() : request.fdate();
        Long id = IdWorker.getId();
        String number = StringUtils.hasText(request.fnumber())
                ? request.fnumber().trim()
                : buildNumber(date, id);
        ensureNumberUnique(tenant, number);

        String stage = normalizeOpenStage(request.fstage());
        BigDecimal probability = normalizeProbability(
                request.fprobability(), stage);

        CrmOpportunityEntity entity = new CrmOpportunityEntity();
        entity.setFid(id);
        entity.setFtenantId(tenant);
        entity.setForgId(request.forgId());
        entity.setFnumber(number);
        entity.setFdate(date);
        entity.setFleadId(request.fleadId());
        entity.setFbusinessPartnerId(partner.fid());
        entity.setFbusinessPartnerCode(partner.fcode());
        entity.setFbusinessPartnerName(partner.fname());
        entity.setFname(request.fname().trim());
        entity.setFownerId(request.fownerId() == null
                ? operatorId : request.fownerId());
        entity.setFcurrencyCode(request.fcurrencyCode().trim().toUpperCase());
        entity.setFexpectedAmount(money(request.fexpectedAmount()));
        entity.setFexpectedCloseDate(request.fexpectedCloseDate());
        entity.setFstage(stage);
        entity.setFprobability(probability);
        entity.setFnextActionDate(request.fnextActionDate());
        entity.setFstatus(STATUS_OPEN);
        entity.setFcreateBy(operatorId);
        entity.setFcreateTime(LocalDateTime.now());
        entity.setFmodifyBy(operatorId);
        entity.setFmodifyTime(entity.getFcreateTime());
        entity.setFdeleteFlag(0);
        entity.setFversion(0);
        requireOne(mapper.insert(entity), "CRM Opportunity");

        if (lead != null) {
            lead.setFstatus("CONVERTED");
            lead.setFconvertedBusinessPartnerId(partner.fid());
            lead.setFconvertedOpportunityId(entity.getFid());
            lead.setFmodifyBy(operatorId);
            lead.setFmodifyTime(LocalDateTime.now());
            requireOne(leadMapper.updateById(lead), "CRM Lead");

            outboxService.append(
                    tenant,
                    entity.getForgId(),
                    "CRM",
                    "CRM_LEAD_CONVERTED",
                    "CRM_LEAD",
                    lead.getFid(),
                    version(lead.getFversion()),
                    "ERP_CRM_LEAD",
                    lead.getFnumber(),
                    lead.getFdate(),
                    operatorId,
                    leadConvertedPayload(lead, entity)
            );
        }

        outboxService.append(
                tenant,
                entity.getForgId(),
                "CRM",
                "CRM_OPPORTUNITY_CREATED",
                "CRM_OPPORTUNITY",
                entity.getFid(),
                version(entity.getFversion()),
                "ERP_CRM_OPPORTUNITY",
                entity.getFnumber(),
                entity.getFdate(),
                operatorId,
                payload(entity)
        );
        return entity;
    }

    @Transactional(rollbackFor = Exception.class)
    public CrmOpportunityEntity update(
            Long fid,
            UpdateRequest request,
            Long operatorId
    ) {
        String tenant = requireTenant(request.ftenantId());
        CrmOpportunityEntity entity =
                requireOpportunity(fid, tenant, true);
        ensureOpen(entity);

        entity.setForgId(request.forgId());
        entity.setFname(request.fname().trim());
        entity.setFownerId(request.fownerId() == null
                ? entity.getFownerId() : request.fownerId());
        entity.setFcurrencyCode(request.fcurrencyCode().trim().toUpperCase());
        entity.setFexpectedAmount(money(request.fexpectedAmount()));
        entity.setFexpectedCloseDate(request.fexpectedCloseDate());
        entity.setFnextActionDate(request.fnextActionDate());
        touch(entity, operatorId);
        requireOne(mapper.updateById(entity), "CRM Opportunity");
        return entity;
    }

    @Transactional(rollbackFor = Exception.class)
    public CrmOpportunityEntity changeStage(
            Long fid,
            String tenantId,
            String stage,
            BigDecimal probability,
            LocalDate nextActionDate,
            Long operatorId
    ) {
        CrmOpportunityEntity entity =
                requireOpportunity(fid, tenantId, true);
        ensureOpen(entity);
        String normalizedStage = normalizeOpenStage(stage);
        entity.setFstage(normalizedStage);
        entity.setFprobability(normalizeProbability(
                probability, normalizedStage));
        entity.setFnextActionDate(nextActionDate);
        touch(entity, operatorId);
        requireOne(mapper.updateById(entity), "CRM Opportunity");
        return entity;
    }

    @Transactional(rollbackFor = Exception.class)
    public CrmOpportunityEntity win(
            Long fid,
            String tenantId,
            Long operatorId
    ) {
        CrmOpportunityEntity entity =
                requireOpportunity(fid, tenantId, true);
        if (STATUS_WON.equals(entity.getFstatus())) {
            return entity;
        }
        ensureOpen(entity);
        entity.setFstatus(STATUS_WON);
        entity.setFstage(STAGE_WON);
        entity.setFprobability(new BigDecimal("100.00"));
        entity.setFwonTime(LocalDateTime.now());
        entity.setFlostReason(null);
        entity.setFlostTime(null);
        touch(entity, operatorId);
        requireOne(mapper.updateById(entity), "CRM Opportunity");

        outboxService.append(
                entity.getFtenantId(),
                entity.getForgId(),
                "CRM",
                "CRM_OPPORTUNITY_WON",
                "CRM_OPPORTUNITY",
                entity.getFid(),
                version(entity.getFversion()),
                "ERP_CRM_OPPORTUNITY",
                entity.getFnumber(),
                entity.getFdate(),
                operatorId,
                payload(entity)
        );
        return entity;
    }

    @Transactional(rollbackFor = Exception.class)
    public CrmOpportunityEntity lose(
            Long fid,
            String tenantId,
            String reason,
            Long operatorId
    ) {
        CrmOpportunityEntity entity =
                requireOpportunity(fid, tenantId, true);
        if (STATUS_LOST.equals(entity.getFstatus())) {
            return entity;
        }
        ensureOpen(entity);
        if (!StringUtils.hasText(reason)) {
            throw new BizException("商机丢单原因不能为空");
        }
        entity.setFstatus(STATUS_LOST);
        entity.setFstage(STAGE_LOST);
        entity.setFprobability(BigDecimal.ZERO.setScale(2));
        entity.setFlostReason(reason.trim());
        entity.setFlostTime(LocalDateTime.now());
        entity.setFwonTime(null);
        touch(entity, operatorId);
        requireOne(mapper.updateById(entity), "CRM Opportunity");

        outboxService.append(
                entity.getFtenantId(),
                entity.getForgId(),
                "CRM",
                "CRM_OPPORTUNITY_LOST",
                "CRM_OPPORTUNITY",
                entity.getFid(),
                version(entity.getFversion()),
                "ERP_CRM_OPPORTUNITY",
                entity.getFnumber(),
                entity.getFdate(),
                operatorId,
                payload(entity)
        );
        return entity;
    }

    private CrmOpportunityEntity requireOpportunity(
            Long fid,
            String tenantId,
            boolean forUpdate
    ) {
        String tenant = requireTenant(tenantId);
        CrmOpportunityEntity entity = forUpdate
                ? mapper.selectByIdForUpdate(fid, tenant)
                : mapper.selectOne(
                        new LambdaQueryWrapper<CrmOpportunityEntity>()
                                .eq(CrmOpportunityEntity::getFid, fid)
                                .eq(CrmOpportunityEntity::getFtenantId, tenant)
                                .last("limit 1"));
        if (entity == null) {
            throw new BizException("CRM Opportunity 不存在: " + fid);
        }
        return entity;
    }

    private void ensureOpen(CrmOpportunityEntity entity) {
        if (!STATUS_OPEN.equals(entity.getFstatus())) {
            throw new BizException("只有 OPEN 商机允许修改");
        }
    }

    private String normalizeOpenStage(String value) {
        String stage = StringUtils.hasText(value)
                ? value.trim().toUpperCase()
                : STAGE_QUALIFICATION;
        if (!OPEN_STAGES.contains(stage)) {
            throw new BizException(
                    "OPEN 商机阶段仅支持 DISCOVERY/QUALIFICATION/PROPOSAL/NEGOTIATION");
        }
        return stage;
    }

    private BigDecimal normalizeProbability(
            BigDecimal value,
            String stage
    ) {
        BigDecimal probability = value == null
                ? defaultProbability(stage)
                : value.setScale(2, RoundingMode.HALF_UP);
        if (probability.compareTo(BigDecimal.ZERO) < 0
                || probability.compareTo(new BigDecimal("100")) >= 0) {
            throw new BizException("OPEN 商机概率必须在 [0,100) 范围");
        }
        return probability;
    }

    private BigDecimal defaultProbability(String stage) {
        return switch (stage) {
            case STAGE_DISCOVERY -> new BigDecimal("10.00");
            case STAGE_PROPOSAL -> new BigDecimal("50.00");
            case STAGE_NEGOTIATION -> new BigDecimal("75.00");
            default -> new BigDecimal("25.00");
        };
    }

    private void ensureNumberUnique(String tenant, String number) {
        Long count = mapper.selectCount(
                new LambdaQueryWrapper<CrmOpportunityEntity>()
                        .eq(CrmOpportunityEntity::getFtenantId, tenant)
                        .eq(CrmOpportunityEntity::getFnumber, number));
        if (count != null && count > 0) {
            throw new BizException("CRM Opportunity 单号已存在: " + number);
        }
    }

    private Map<String, Object> payload(
            CrmOpportunityEntity entity
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("opportunityId", entity.getFid());
        payload.put("opportunityNo", entity.getFnumber());
        payload.put("leadId", entity.getFleadId());
        payload.put("businessPartnerId", entity.getFbusinessPartnerId());
        payload.put("businessPartnerCode", entity.getFbusinessPartnerCode());
        payload.put("businessPartnerName", entity.getFbusinessPartnerName());
        payload.put("name", entity.getFname());
        payload.put("stage", entity.getFstage());
        payload.put("status", entity.getFstatus());
        payload.put("probability", entity.getFprobability());
        payload.put("expectedAmount", entity.getFexpectedAmount());
        payload.put("currencyCode", entity.getFcurrencyCode());
        payload.put("expectedCloseDate", entity.getFexpectedCloseDate());
        return payload;
    }

    private Map<String, Object> leadConvertedPayload(
            CrmLeadEntity lead,
            CrmOpportunityEntity opportunity
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("leadId", lead.getFid());
        payload.put("leadNo", lead.getFnumber());
        payload.put("businessPartnerId", opportunity.getFbusinessPartnerId());
        payload.put("opportunityId", opportunity.getFid());
        payload.put("opportunityNo", opportunity.getFnumber());
        return payload;
    }

    private long version(Integer value) {
        return value == null ? 0L : value.longValue();
    }

    private BigDecimal money(BigDecimal value) {
        return value == null
                ? BigDecimal.ZERO.setScale(2)
                : value.setScale(2, RoundingMode.HALF_UP);
    }

    private String buildNumber(LocalDate date, Long id) {
        String raw = String.valueOf(id);
        String suffix = raw.substring(Math.max(0, raw.length() - 8));
        return "OPP" + date.format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + suffix;
    }

    private String requireTenant(String tenantId) {
        if (!StringUtils.hasText(tenantId)) {
            throw new BizException("tenantId 不能为空");
        }
        return tenantId.trim();
    }

    private void touch(
            CrmOpportunityEntity entity,
            Long operatorId
    ) {
        entity.setFmodifyBy(operatorId);
        entity.setFmodifyTime(LocalDateTime.now());
    }

    private void requireOne(int affected, String objectName) {
        if (affected != 1) {
            throw new BizException(objectName + "已被其他请求修改，请刷新后重试");
        }
    }
}
