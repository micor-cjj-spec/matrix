package single.cjj.bizfi.partner.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.exception.BizException;
import single.cjj.bizfi.partner.dto.BusinessPartnerContracts.BusinessPartnerDetail;
import single.cjj.bizfi.partner.dto.BusinessPartnerContracts.LegacyPartyRequest;
import single.cjj.bizfi.partner.dto.BusinessPartnerContracts.LegacyPartyResponse;
import single.cjj.bizfi.partner.entity.BusinessPartnerEntity;
import single.cjj.bizfi.partner.entity.BusinessPartnerRoleEntity;
import single.cjj.bizfi.partner.mapper.BusinessPartnerMapper;
import single.cjj.bizfi.partner.mapper.BusinessPartnerRoleMapper;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class BusinessPartnerService {

    public static final String ROLE_CUSTOMER = "CUSTOMER";
    public static final String ROLE_SUPPLIER = "SUPPLIER";

    private static final String PARTNER_ORGANIZATION = "ORGANIZATION";
    private static final String LIFECYCLE_DRAFT = "DRAFT";
    private static final String LIFECYCLE_ACTIVE = "ACTIVE";
    private static final String APPROVAL_DRAFT = "DRAFT";
    private static final String APPROVAL_SUBMITTED = "SUBMITTED";
    private static final String APPROVAL_AUDITED = "AUDITED";
    private static final String APPROVAL_REJECTED = "REJECTED";
    private static final String ROLE_ENABLED = "ENABLED";

    private final BusinessPartnerMapper partnerMapper;
    private final BusinessPartnerRoleMapper roleMapper;

    public BusinessPartnerService(
            BusinessPartnerMapper partnerMapper,
            BusinessPartnerRoleMapper roleMapper
    ) {
        this.partnerMapper = partnerMapper;
        this.roleMapper = roleMapper;
    }

    public List<LegacyPartyResponse> listLegacyRole(
            String tenantId,
            String roleType
    ) {
        String tenant = normalizeTenant(tenantId);
        String role = normalizeRole(roleType);
        List<BusinessPartnerRoleEntity> roles = roleMapper.selectList(
                new LambdaQueryWrapper<BusinessPartnerRoleEntity>()
                        .eq(BusinessPartnerRoleEntity::getFtenantId, tenant)
                        .eq(BusinessPartnerRoleEntity::getFroleType, role)
                        .eq(BusinessPartnerRoleEntity::getFstatus, ROLE_ENABLED)
                        .orderByAsc(BusinessPartnerRoleEntity::getFid)
        );
        if (roles.isEmpty()) {
            return List.of();
        }
        Set<Long> ids = new LinkedHashSet<>();
        for (BusinessPartnerRoleEntity item : roles) {
            ids.add(item.getFbusinessPartnerId());
        }
        return partnerMapper.selectBatchIds(ids).stream()
                .filter(item -> tenant.equals(item.getFtenantId()))
                .sorted(Comparator.comparing(
                                BusinessPartnerEntity::getFcode,
                                Comparator.nullsLast(String::compareTo))
                        .thenComparing(BusinessPartnerEntity::getFid))
                .map(item -> toLegacy(item, role))
                .toList();
    }

    public LegacyPartyResponse detailLegacyRole(
            Long partnerId,
            String tenantId,
            String roleType
    ) {
        BusinessPartnerEntity partner =
                requirePartnerWithRole(partnerId, tenantId, roleType);
        return toLegacy(partner, normalizeRole(roleType));
    }

    @Transactional(rollbackFor = Exception.class)
    public LegacyPartyResponse createLegacyRole(
            String tenantId,
            String roleType,
            LegacyPartyRequest request,
            Long operatorId
    ) {
        String tenant = normalizeTenant(tenantId);
        String role = normalizeRole(roleType);
        validateName(request);

        String requestedCode = trimToNull(request.fcode());
        String creditCode = trimToNull(request.funifiedSocialCreditCode());

        BusinessPartnerEntity byCode = requestedCode == null
                ? null : findByCode(tenant, requestedCode);
        BusinessPartnerEntity byCredit = creditCode == null
                ? null : findByCreditCode(tenant, creditCode);

        if (byCode != null && byCredit != null
                && !byCode.getFid().equals(byCredit.getFid())) {
            throw new BizException("客商编码与统一社会信用代码命中了不同主体，请先合并主数据");
        }

        BusinessPartnerEntity partner = byCode != null ? byCode : byCredit;
        if (partner == null) {
            partner = createPartner(
                    tenant, role, request, operatorId, requestedCode, creditCode);
        } else {
            if (findRole(tenant, partner.getFid(), role) != null) {
                throw new BizException(roleLabel(role) + "已存在: " + partner.getFcode());
            }
            if (creditCode != null
                    && partner.getFunifiedSocialCreditCode() == null
                    && !APPROVAL_AUDITED.equals(partner.getFapprovalStatus())) {
                partner.setFunifiedSocialCreditCode(creditCode);
                touch(partner, operatorId);
                requireOne(partnerMapper.updateById(partner), "BusinessPartner");
            }
        }

        BusinessPartnerRoleEntity roleEntity = new BusinessPartnerRoleEntity();
        roleEntity.setFid(IdWorker.getId());
        roleEntity.setFtenantId(tenant);
        roleEntity.setFbusinessPartnerId(partner.getFid());
        roleEntity.setFroleType(role);
        roleEntity.setFstatus(ROLE_ENABLED);
        roleEntity.setFcreateBy(operatorId);
        roleEntity.setFcreateTime(LocalDateTime.now());
        roleEntity.setFmodifyBy(operatorId);
        roleEntity.setFmodifyTime(LocalDateTime.now());
        roleEntity.setFdeleteFlag(0);
        roleEntity.setFversion(0);
        requireOne(roleMapper.insert(roleEntity), roleLabel(role) + "角色");

        return toLegacy(partner, role);
    }

    @Transactional(rollbackFor = Exception.class)
    public LegacyPartyResponse updateLegacyRole(
            String tenantId,
            String roleType,
            LegacyPartyRequest request,
            Long operatorId
    ) {
        if (request == null || request.fid() == null) {
            throw new BizException("fid 不能为空");
        }
        validateName(request);
        String tenant = normalizeTenant(tenantId);
        String role = normalizeRole(roleType);
        BusinessPartnerEntity partner =
                requirePartnerWithRole(request.fid(), tenant, role);
        if (APPROVAL_AUDITED.equals(partner.getFapprovalStatus())) {
            throw new BizException("已审核客商不允许通过兼容接口修改");
        }

        String code = trimToNull(request.fcode());
        if (code != null && !code.equals(partner.getFcode())) {
            BusinessPartnerEntity duplicate = findByCode(tenant, code);
            if (duplicate != null && !duplicate.getFid().equals(partner.getFid())) {
                throw new BizException("客商编码已存在: " + code);
            }
            partner.setFcode(code);
        }
        partner.setFname(request.fname().trim());

        String creditCode = trimToNull(request.funifiedSocialCreditCode());
        if (creditCode != null
                && !creditCode.equals(partner.getFunifiedSocialCreditCode())) {
            BusinessPartnerEntity duplicate = findByCreditCode(tenant, creditCode);
            if (duplicate != null && !duplicate.getFid().equals(partner.getFid())) {
                throw new BizException("统一社会信用代码已存在: " + creditCode);
            }
            partner.setFunifiedSocialCreditCode(creditCode);
        }

        touch(partner, operatorId);
        requireOne(partnerMapper.updateById(partner), "BusinessPartner");
        return toLegacy(partnerMapper.selectById(partner.getFid()), role);
    }

    @Transactional(rollbackFor = Exception.class)
    public LegacyPartyResponse submitLegacyRole(
            Long partnerId,
            String tenantId,
            String roleType,
            Long operatorId
    ) {
        String role = normalizeRole(roleType);
        BusinessPartnerEntity partner =
                requirePartnerWithRole(partnerId, tenantId, role);
        if (!APPROVAL_DRAFT.equals(partner.getFapprovalStatus())
                && !APPROVAL_REJECTED.equals(partner.getFapprovalStatus())) {
            throw new BizException("仅草稿或已驳回客商可提交");
        }
        partner.setFapprovalStatus(APPROVAL_SUBMITTED);
        touch(partner, operatorId);
        requireOne(partnerMapper.updateById(partner), "BusinessPartner");
        return toLegacy(partner, role);
    }

    @Transactional(rollbackFor = Exception.class)
    public LegacyPartyResponse auditLegacyRole(
            Long partnerId,
            String tenantId,
            String roleType,
            Long operatorId
    ) {
        String role = normalizeRole(roleType);
        BusinessPartnerEntity partner =
                requirePartnerWithRole(partnerId, tenantId, role);
        if (!APPROVAL_SUBMITTED.equals(partner.getFapprovalStatus())) {
            throw new BizException("仅已提交客商可审核");
        }
        partner.setFapprovalStatus(APPROVAL_AUDITED);
        partner.setFstatus(LIFECYCLE_ACTIVE);
        touch(partner, operatorId);
        requireOne(partnerMapper.updateById(partner), "BusinessPartner");
        return toLegacy(partner, role);
    }

    @Transactional(rollbackFor = Exception.class)
    public LegacyPartyResponse rejectLegacyRole(
            Long partnerId,
            String tenantId,
            String roleType,
            Long operatorId
    ) {
        String role = normalizeRole(roleType);
        BusinessPartnerEntity partner =
                requirePartnerWithRole(partnerId, tenantId, role);
        if (!APPROVAL_SUBMITTED.equals(partner.getFapprovalStatus())) {
            throw new BizException("仅已提交客商可驳回");
        }
        partner.setFapprovalStatus(APPROVAL_REJECTED);
        partner.setFstatus(LIFECYCLE_DRAFT);
        touch(partner, operatorId);
        requireOne(partnerMapper.updateById(partner), "BusinessPartner");
        return toLegacy(partner, role);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean deleteLegacyRole(
            Long partnerId,
            String tenantId,
            String roleType
    ) {
        String tenant = normalizeTenant(tenantId);
        String role = normalizeRole(roleType);
        BusinessPartnerEntity partner =
                requirePartnerWithRole(partnerId, tenant, role);
        if (APPROVAL_AUDITED.equals(partner.getFapprovalStatus())) {
            throw new BizException("已审核客商不允许删除");
        }

        BusinessPartnerRoleEntity roleEntity =
                requireRole(tenant, partnerId, role);
        requireOne(roleMapper.deleteById(roleEntity.getFid()), roleLabel(role) + "角色");

        Long remaining = roleMapper.selectCount(
                new LambdaQueryWrapper<BusinessPartnerRoleEntity>()
                        .eq(BusinessPartnerRoleEntity::getFtenantId, tenant)
                        .eq(BusinessPartnerRoleEntity::getFbusinessPartnerId, partnerId)
                        .eq(BusinessPartnerRoleEntity::getFstatus, ROLE_ENABLED)
        );
        if (remaining == null || remaining == 0) {
            requireOne(partnerMapper.deleteById(partnerId), "BusinessPartner");
        }
        return true;
    }

    public BusinessPartnerDetail detail(
            Long partnerId,
            String tenantId
    ) {
        String tenant = normalizeTenant(tenantId);
        BusinessPartnerEntity partner = requirePartner(partnerId, tenant);
        List<String> roles = roleMapper.selectList(
                        new LambdaQueryWrapper<BusinessPartnerRoleEntity>()
                                .eq(BusinessPartnerRoleEntity::getFtenantId, tenant)
                                .eq(BusinessPartnerRoleEntity::getFbusinessPartnerId, partnerId)
                                .eq(BusinessPartnerRoleEntity::getFstatus, ROLE_ENABLED)
                                .orderByAsc(BusinessPartnerRoleEntity::getFroleType))
                .stream()
                .map(BusinessPartnerRoleEntity::getFroleType)
                .toList();
        return toDetail(partner, roles);
    }

    public BusinessPartnerDetail resolveByCode(
            String tenantId,
            String code
    ) {
        String tenant = normalizeTenant(tenantId);
        String normalizedCode = trimToNull(code);
        if (normalizedCode == null) {
            throw new BizException("code 不能为空");
        }
        BusinessPartnerEntity partner = findByCode(tenant, normalizedCode);
        if (partner == null) {
            throw new BizException("BusinessPartner 不存在: " + normalizedCode);
        }
        return detail(partner.getFid(), tenant);
    }

    private BusinessPartnerEntity createPartner(
            String tenant,
            String role,
            LegacyPartyRequest request,
            Long operatorId,
            String requestedCode,
            String creditCode
    ) {
        Long id = IdWorker.getId();
        BusinessPartnerEntity partner = new BusinessPartnerEntity();
        partner.setFid(id);
        partner.setFtenantId(tenant);
        partner.setFcode(requestedCode == null
                ? generatedCode(role, id) : requestedCode);
        partner.setFname(request.fname().trim());
        partner.setFpartnerType(PARTNER_ORGANIZATION);
        partner.setFunifiedSocialCreditCode(creditCode);
        partner.setFstatus(LIFECYCLE_DRAFT);
        partner.setFapprovalStatus(APPROVAL_DRAFT);
        partner.setFcreateBy(operatorId);
        partner.setFcreateTime(LocalDateTime.now());
        partner.setFmodifyBy(operatorId);
        partner.setFmodifyTime(LocalDateTime.now());
        partner.setFdeleteFlag(0);
        partner.setFversion(0);
        requireOne(partnerMapper.insert(partner), "BusinessPartner");
        return partner;
    }

    private BusinessPartnerEntity requirePartnerWithRole(
            Long partnerId,
            String tenantId,
            String roleType
    ) {
        String tenant = normalizeTenant(tenantId);
        String role = normalizeRole(roleType);
        BusinessPartnerEntity partner = requirePartner(partnerId, tenant);
        requireRole(tenant, partnerId, role);
        return partner;
    }

    private BusinessPartnerEntity requirePartner(
            Long partnerId,
            String tenant
    ) {
        if (partnerId == null) {
            throw new BizException("BusinessPartner ID 不能为空");
        }
        BusinessPartnerEntity partner = partnerMapper.selectOne(
                new LambdaQueryWrapper<BusinessPartnerEntity>()
                        .eq(BusinessPartnerEntity::getFid, partnerId)
                        .eq(BusinessPartnerEntity::getFtenantId, tenant)
                        .last("limit 1")
        );
        if (partner == null) {
            throw new BizException("BusinessPartner 不存在: " + partnerId);
        }
        return partner;
    }

    private BusinessPartnerRoleEntity requireRole(
            String tenant,
            Long partnerId,
            String role
    ) {
        BusinessPartnerRoleEntity entity = findRole(tenant, partnerId, role);
        if (entity == null) {
            throw new BizException(roleLabel(role) + "角色不存在: " + partnerId);
        }
        return entity;
    }

    private BusinessPartnerRoleEntity findRole(
            String tenant,
            Long partnerId,
            String role
    ) {
        return roleMapper.selectOne(
                new LambdaQueryWrapper<BusinessPartnerRoleEntity>()
                        .eq(BusinessPartnerRoleEntity::getFtenantId, tenant)
                        .eq(BusinessPartnerRoleEntity::getFbusinessPartnerId, partnerId)
                        .eq(BusinessPartnerRoleEntity::getFroleType, role)
                        .eq(BusinessPartnerRoleEntity::getFstatus, ROLE_ENABLED)
                        .last("limit 1")
        );
    }

    private BusinessPartnerEntity findByCode(
            String tenant,
            String code
    ) {
        return partnerMapper.selectOne(
                new LambdaQueryWrapper<BusinessPartnerEntity>()
                        .eq(BusinessPartnerEntity::getFtenantId, tenant)
                        .eq(BusinessPartnerEntity::getFcode, code)
                        .last("limit 1")
        );
    }

    private BusinessPartnerEntity findByCreditCode(
            String tenant,
            String creditCode
    ) {
        return partnerMapper.selectOne(
                new LambdaQueryWrapper<BusinessPartnerEntity>()
                        .eq(BusinessPartnerEntity::getFtenantId, tenant)
                        .eq(BusinessPartnerEntity::getFunifiedSocialCreditCode, creditCode)
                        .last("limit 1")
        );
    }

    private LegacyPartyResponse toLegacy(
            BusinessPartnerEntity partner,
            String role
    ) {
        return new LegacyPartyResponse(
                partner.getFid(),
                partner.getFname(),
                partner.getFcode(),
                partner.getFapprovalStatus(),
                partner.getFid(),
                role,
                partner.getFstatus(),
                partner.getFapprovalStatus(),
                partner.getFunifiedSocialCreditCode()
        );
    }

    private BusinessPartnerDetail toDetail(
            BusinessPartnerEntity partner,
            List<String> roles
    ) {
        return new BusinessPartnerDetail(
                partner.getFid(),
                partner.getFtenantId(),
                partner.getFcode(),
                partner.getFname(),
                partner.getFpartnerType(),
                partner.getFunifiedSocialCreditCode(),
                partner.getFstatus(),
                partner.getFapprovalStatus(),
                roles
        );
    }

    private void validateName(LegacyPartyRequest request) {
        if (request == null || !StringUtils.hasText(request.fname())) {
            throw new BizException("名称不能为空");
        }
    }

    private String normalizeTenant(String tenantId) {
        return StringUtils.hasText(tenantId) ? tenantId.trim() : "default";
    }

    private String normalizeRole(String roleType) {
        String role = StringUtils.hasText(roleType)
                ? roleType.trim().toUpperCase() : "";
        if (!ROLE_CUSTOMER.equals(role) && !ROLE_SUPPLIER.equals(role)) {
            throw new BizException("兼容接口仅支持 CUSTOMER / SUPPLIER");
        }
        return role;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String generatedCode(String role, Long id) {
        String raw = String.valueOf(id);
        String suffix = raw.substring(Math.max(0, raw.length() - 10));
        return (ROLE_CUSTOMER.equals(role) ? "CUST-" : "SUP-") + suffix;
    }

    private String roleLabel(String role) {
        return ROLE_CUSTOMER.equals(role) ? "客户" : "供应商";
    }

    private void touch(BusinessPartnerEntity partner, Long operatorId) {
        partner.setFmodifyBy(operatorId);
        partner.setFmodifyTime(LocalDateTime.now());
    }

    private void requireOne(int affected, String objectName) {
        if (affected != 1) {
            throw new BizException(objectName + "已被其他请求修改，请刷新后重试");
        }
    }
}
