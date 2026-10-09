package single.cjj.erp.crm.lead.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.exception.BizException;
import single.cjj.erp.crm.lead.dto.CrmLeadContracts.CreateRequest;
import single.cjj.erp.crm.lead.dto.CrmLeadContracts.UpdateRequest;
import single.cjj.erp.crm.lead.entity.CrmLeadEntity;
import single.cjj.erp.crm.lead.mapper.CrmLeadMapper;
import single.cjj.erp.event.service.BusinessEventOutboxService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class CrmLeadService {

    public static final String STATUS_NEW = "NEW";
    public static final String STATUS_QUALIFYING = "QUALIFYING";
    public static final String STATUS_QUALIFIED = "QUALIFIED";
    public static final String STATUS_CONVERTED = "CONVERTED";
    public static final String STATUS_DISQUALIFIED = "DISQUALIFIED";

    private static final String EVENT_QUALIFIED = "CRM_LEAD_QUALIFIED";

    private final CrmLeadMapper mapper;
    private final BusinessEventOutboxService outboxService;

    public CrmLeadService(
            CrmLeadMapper mapper,
            BusinessEventOutboxService outboxService
    ) {
        this.mapper = mapper;
        this.outboxService = outboxService;
    }

    public CrmLeadEntity detail(Long fid, String tenantId) {
        return requireLead(fid, tenantId, false);
    }

    public IPage<CrmLeadEntity> page(
            String tenantId,
            Long orgId,
            int page,
            int size,
            String number,
            String name,
            String source,
            Long ownerId,
            String status
    ) {
        String tenant = requireTenant(tenantId);
        return mapper.selectPage(
                new Page<>(Math.max(page, 1), Math.max(size, 1)),
                new LambdaQueryWrapper<CrmLeadEntity>()
                        .eq(CrmLeadEntity::getFtenantId, tenant)
                        .eq(orgId != null, CrmLeadEntity::getForgId, orgId)
                        .like(StringUtils.hasText(number), CrmLeadEntity::getFnumber, number)
                        .like(StringUtils.hasText(name), CrmLeadEntity::getFname, name)
                        .eq(StringUtils.hasText(source), CrmLeadEntity::getFsource, source)
                        .eq(ownerId != null, CrmLeadEntity::getFownerId, ownerId)
                        .eq(StringUtils.hasText(status), CrmLeadEntity::getFstatus, status)
                        .orderByDesc(CrmLeadEntity::getFdate)
                        .orderByDesc(CrmLeadEntity::getFid)
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public CrmLeadEntity create(CreateRequest request, Long operatorId) {
        String tenant = requireTenant(request.ftenantId());
        LocalDate date = request.fdate() == null ? LocalDate.now() : request.fdate();
        Long id = IdWorker.getId();
        String number = StringUtils.hasText(request.fnumber())
                ? request.fnumber().trim()
                : buildNumber(date, id);
        ensureNumberUnique(tenant, number);

        BigDecimal estimatedAmount = money(request.festimatedAmount());
        String currency = normalizeCurrency(
                request.fcurrencyCode(), estimatedAmount);

        CrmLeadEntity entity = new CrmLeadEntity();
        entity.setFid(id);
        entity.setFtenantId(tenant);
        entity.setForgId(request.forgId());
        entity.setFnumber(number);
        entity.setFdate(date);
        entity.setFname(request.fname().trim());
        entity.setFcompanyName(trimToNull(request.fcompanyName()));
        entity.setFcontactName(trimToNull(request.fcontactName()));
        entity.setFcontactPhone(trimToNull(request.fcontactPhone()));
        entity.setFcontactEmail(trimToNull(request.fcontactEmail()));
        entity.setFsource(trimUpperToNull(request.fsource()));
        entity.setFownerId(request.fownerId() == null ? operatorId : request.fownerId());
        entity.setFestimatedAmount(estimatedAmount);
        entity.setFcurrencyCode(currency);
        entity.setFnextActionDate(request.fnextActionDate());
        entity.setFstatus(STATUS_NEW);
        entity.setFcreateBy(operatorId);
        entity.setFcreateTime(LocalDateTime.now());
        entity.setFmodifyBy(operatorId);
        entity.setFmodifyTime(entity.getFcreateTime());
        entity.setFdeleteFlag(0);
        entity.setFversion(0);
        requireOne(mapper.insert(entity), "CRM Lead");
        return entity;
    }

    @Transactional(rollbackFor = Exception.class)
    public CrmLeadEntity update(
            Long fid,
            UpdateRequest request,
            Long operatorId
    ) {
        String tenant = requireTenant(request.ftenantId());
        CrmLeadEntity entity = requireLead(fid, tenant, true);
        ensureEditable(entity);

        BigDecimal estimatedAmount = money(request.festimatedAmount());
        entity.setForgId(request.forgId());
        entity.setFname(request.fname().trim());
        entity.setFcompanyName(trimToNull(request.fcompanyName()));
        entity.setFcontactName(trimToNull(request.fcontactName()));
        entity.setFcontactPhone(trimToNull(request.fcontactPhone()));
        entity.setFcontactEmail(trimToNull(request.fcontactEmail()));
        entity.setFsource(trimUpperToNull(request.fsource()));
        entity.setFownerId(request.fownerId() == null
                ? entity.getFownerId() : request.fownerId());
        entity.setFestimatedAmount(estimatedAmount);
        entity.setFcurrencyCode(normalizeCurrency(
                request.fcurrencyCode(), estimatedAmount));
        entity.setFnextActionDate(request.fnextActionDate());
        touch(entity, operatorId);
        requireOne(mapper.updateById(entity), "CRM Lead");
        return entity;
    }

    @Transactional(rollbackFor = Exception.class)
    public CrmLeadEntity startQualification(
            Long fid,
            String tenantId,
            Long operatorId
    ) {
        CrmLeadEntity entity = requireLead(fid, tenantId, true);
        if (STATUS_QUALIFYING.equals(entity.getFstatus())) {
            return entity;
        }
        if (!STATUS_NEW.equals(entity.getFstatus())) {
            throw new BizException("只有 NEW 线索允许进入 QUALIFYING");
        }
        entity.setFstatus(STATUS_QUALIFYING);
        touch(entity, operatorId);
        requireOne(mapper.updateById(entity), "CRM Lead");
        return entity;
    }

    @Transactional(rollbackFor = Exception.class)
    public CrmLeadEntity qualify(
            Long fid,
            String tenantId,
            Long operatorId
    ) {
        CrmLeadEntity entity = requireLead(fid, tenantId, true);
        if (STATUS_QUALIFIED.equals(entity.getFstatus())) {
            return entity;
        }
        if (!STATUS_NEW.equals(entity.getFstatus())
                && !STATUS_QUALIFYING.equals(entity.getFstatus())) {
            throw new BizException("只有 NEW / QUALIFYING 线索允许确认 QUALIFIED");
        }
        entity.setFstatus(STATUS_QUALIFIED);
        entity.setFqualifiedTime(LocalDateTime.now());
        entity.setFdisqualifiedReason(null);
        touch(entity, operatorId);
        requireOne(mapper.updateById(entity), "CRM Lead");

        outboxService.append(
                entity.getFtenantId(),
                entity.getForgId(),
                "CRM",
                EVENT_QUALIFIED,
                "CRM_LEAD",
                entity.getFid(),
                version(entity),
                "ERP_CRM_LEAD",
                entity.getFnumber(),
                entity.getFdate(),
                operatorId,
                payload(entity)
        );
        return entity;
    }

    @Transactional(rollbackFor = Exception.class)
    public CrmLeadEntity disqualify(
            Long fid,
            String tenantId,
            String reason,
            Long operatorId
    ) {
        CrmLeadEntity entity = requireLead(fid, tenantId, true);
        if (STATUS_CONVERTED.equals(entity.getFstatus())) {
            throw new BizException("已转换线索不能标记为 DISQUALIFIED");
        }
        if (STATUS_DISQUALIFIED.equals(entity.getFstatus())) {
            return entity;
        }
        if (!StringUtils.hasText(reason)) {
            throw new BizException("线索无效原因不能为空");
        }
        entity.setFstatus(STATUS_DISQUALIFIED);
        entity.setFdisqualifiedReason(reason.trim());
        touch(entity, operatorId);
        requireOne(mapper.updateById(entity), "CRM Lead");
        return entity;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long fid, String tenantId) {
        CrmLeadEntity entity = requireLead(fid, tenantId, true);
        if (!STATUS_NEW.equals(entity.getFstatus())) {
            throw new BizException("仅 NEW 线索允许删除");
        }
        return mapper.deleteById(fid) > 0;
    }

    private CrmLeadEntity requireLead(
            Long fid,
            String tenantId,
            boolean forUpdate
    ) {
        String tenant = requireTenant(tenantId);
        CrmLeadEntity entity = forUpdate
                ? mapper.selectByIdForUpdate(fid, tenant)
                : mapper.selectOne(new LambdaQueryWrapper<CrmLeadEntity>()
                        .eq(CrmLeadEntity::getFid, fid)
                        .eq(CrmLeadEntity::getFtenantId, tenant)
                        .last("limit 1"));
        if (entity == null) {
            throw new BizException("CRM Lead 不存在: " + fid);
        }
        return entity;
    }

    private void ensureEditable(CrmLeadEntity entity) {
        if (STATUS_CONVERTED.equals(entity.getFstatus())
                || STATUS_DISQUALIFIED.equals(entity.getFstatus())) {
            throw new BizException("已转换或已淘汰线索不允许修改");
        }
    }

    private void ensureNumberUnique(String tenant, String number) {
        Long count = mapper.selectCount(
                new LambdaQueryWrapper<CrmLeadEntity>()
                        .eq(CrmLeadEntity::getFtenantId, tenant)
                        .eq(CrmLeadEntity::getFnumber, number));
        if (count != null && count > 0) {
            throw new BizException("CRM Lead 单号已存在: " + number);
        }
    }

    private Map<String, Object> payload(CrmLeadEntity entity) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("leadId", entity.getFid());
        payload.put("leadNo", entity.getFnumber());
        payload.put("name", entity.getFname());
        payload.put("companyName", entity.getFcompanyName());
        payload.put("source", entity.getFsource());
        payload.put("ownerId", entity.getFownerId());
        payload.put("estimatedAmount", entity.getFestimatedAmount());
        payload.put("currencyCode", entity.getFcurrencyCode());
        return payload;
    }

    private long version(CrmLeadEntity entity) {
        return entity.getFversion() == null
                ? 0L : entity.getFversion().longValue();
    }

    private BigDecimal money(BigDecimal value) {
        return value == null
                ? BigDecimal.ZERO.setScale(2)
                : value.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeCurrency(
            String value,
            BigDecimal amount
    ) {
        String currency = trimUpperToNull(value);
        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0
                && currency == null) {
            throw new BizException("预计金额大于0时 currencyCode 不能为空");
        }
        return currency;
    }

    private String buildNumber(LocalDate date, Long id) {
        String raw = String.valueOf(id);
        String suffix = raw.substring(Math.max(0, raw.length() - 8));
        return "LEAD" + date.format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + suffix;
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

    private String trimUpperToNull(String value) {
        return StringUtils.hasText(value)
                ? value.trim().toUpperCase() : null;
    }

    private void touch(CrmLeadEntity entity, Long operatorId) {
        entity.setFmodifyBy(operatorId);
        entity.setFmodifyTime(LocalDateTime.now());
    }

    private void requireOne(int affected, String objectName) {
        if (affected != 1) {
            throw new BizException(objectName + "已被其他请求修改，请刷新后重试");
        }
    }
}
