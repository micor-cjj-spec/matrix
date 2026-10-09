package single.cjj.bizfi.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.ai.config.KnowledgeAclProperties;
import single.cjj.bizfi.ai.dto.AiKnowledgeAccessResponse;
import single.cjj.bizfi.ai.dto.AiKnowledgeAclRequest;
import single.cjj.bizfi.ai.dto.AiKnowledgeAclResponse;
import single.cjj.bizfi.ai.entity.BizfiAiKnowledgeBase;
import single.cjj.bizfi.ai.entity.BizfiAiKnowledgeBaseAcl;
import single.cjj.bizfi.ai.mapper.BizfiAiKnowledgeBaseAclMapper;
import single.cjj.bizfi.ai.mapper.BizfiAiKnowledgeBaseMapper;
import single.cjj.bizfi.ai.security.AiKnowledgeAclSubjectType;
import single.cjj.bizfi.ai.security.AiKnowledgePermission;
import single.cjj.bizfi.exception.BizException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AiKnowledgeAclService {

    private final BizfiAiKnowledgeBaseAclMapper aclMapper;
    private final BizfiAiKnowledgeBaseMapper baseMapper;
    private final KnowledgeAclProperties properties;

    public AiKnowledgeAclService(
            BizfiAiKnowledgeBaseAclMapper aclMapper,
            BizfiAiKnowledgeBaseMapper baseMapper,
            KnowledgeAclProperties properties
    ) {
        this.aclMapper = aclMapper;
        this.baseMapper = baseMapper;
        this.properties = properties;
    }

    public boolean isEnabled() {
        return Boolean.TRUE.equals(properties.getEnabled());
    }

    /**
     * Returns null when ACL filtering is disabled or the current principal is a system administrator.
     */
    public Set<String> resolveAccessibleBaseIds(AiKnowledgePermission required) {
        PrincipalSubjects principal = currentPrincipal();
        if (!isEnabled() || isSystemAdmin(principal)) {
            return null;
        }
        if (principal.userId() == null) {
            return Set.of();
        }
        return loadMatchingEntries(principal, null).stream()
                .filter(entry -> permissionAllows(entry.getFpermission(), required))
                .map(BizfiAiKnowledgeBaseAcl::getFkbid)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public AiKnowledgePermission effectivePermission(String kbId) {
        PrincipalSubjects principal = currentPrincipal();
        if (!isEnabled() || isSystemAdmin(principal)) {
            return AiKnowledgePermission.OWNER;
        }
        if (principal.userId() == null) {
            return null;
        }
        return highestPermission(loadMatchingEntries(principal, normalizeKbId(kbId)));
    }

    public void assertCanView(String kbId) {
        assertPermission(kbId, AiKnowledgePermission.VIEWER);
    }

    public void assertCanEdit(String kbId) {
        assertPermission(kbId, AiKnowledgePermission.EDITOR);
    }

    public void assertCanAdmin(String kbId) {
        assertPermission(kbId, AiKnowledgePermission.ADMIN);
    }

    public void assertCanOwn(String kbId) {
        assertPermission(kbId, AiKnowledgePermission.OWNER);
    }

    public AiKnowledgeAccessResponse currentAccess(String kbId) {
        String normalizedKbId = requireBaseId(kbId);
        AiKnowledgePermission permission = effectivePermission(normalizedKbId);
        return new AiKnowledgeAccessResponse(
                normalizedKbId,
                isEnabled(),
                permission == null ? "NONE" : permission.name(),
                permission != null && permission.allows(AiKnowledgePermission.VIEWER),
                permission != null && permission.allows(AiKnowledgePermission.EDITOR),
                permission != null && permission.allows(AiKnowledgePermission.ADMIN),
                permission != null && permission.allows(AiKnowledgePermission.OWNER)
        );
    }

    public List<AiKnowledgeAclResponse> listEntries(String kbId) {
        String normalizedKbId = requireBaseId(kbId);
        assertCanManageAcl(normalizedKbId, AiKnowledgePermission.ADMIN);
        return aclMapper.selectList(new LambdaQueryWrapper<BizfiAiKnowledgeBaseAcl>()
                        .eq(BizfiAiKnowledgeBaseAcl::getFkbid, normalizedKbId)
                        .orderByAsc(BizfiAiKnowledgeBaseAcl::getFsubjecttype)
                        .orderByAsc(BizfiAiKnowledgeBaseAcl::getFsubjectid))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public AiKnowledgeAclResponse grant(String kbId, AiKnowledgeAclRequest request) {
        String normalizedKbId = requireBaseId(kbId);
        if (request == null) {
            throw new BizException("ACL授权内容不能为空");
        }
        AiKnowledgeAclSubjectType subjectType = AiKnowledgeAclSubjectType.parse(request.getSubjectType());
        String subjectId = normalizeSubjectId(subjectType, request.getSubjectId());
        AiKnowledgePermission permission = AiKnowledgePermission.parse(request.getPermission());
        assertCanManageAcl(
                normalizedKbId,
                permission == AiKnowledgePermission.OWNER
                        ? AiKnowledgePermission.OWNER
                        : AiKnowledgePermission.ADMIN
        );

        BizfiAiKnowledgeBaseAcl existing = aclMapper.selectOne(new LambdaQueryWrapper<BizfiAiKnowledgeBaseAcl>()
                .eq(BizfiAiKnowledgeBaseAcl::getFkbid, normalizedKbId)
                .eq(BizfiAiKnowledgeBaseAcl::getFsubjecttype, subjectType.name())
                .eq(BizfiAiKnowledgeBaseAcl::getFsubjectid, subjectId)
                .last("limit 1"));

        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            existing = new BizfiAiKnowledgeBaseAcl();
            existing.setFkbid(normalizedKbId);
            existing.setFsubjecttype(subjectType.name());
            existing.setFsubjectid(subjectId);
            existing.setFpermission(permission.name());
            existing.setFcreatedby(requireCurrentUserId());
            existing.setFcreatetime(now);
            existing.setFmodifytime(now);
            aclMapper.insert(existing);
        } else {
            AiKnowledgePermission previous = parseStoredPermission(existing.getFpermission());
            if (previous == AiKnowledgePermission.OWNER && permission != AiKnowledgePermission.OWNER) {
                assertCanManageAcl(normalizedKbId, AiKnowledgePermission.OWNER);
                assertAnotherOwnerExists(normalizedKbId, existing.getFid());
            }
            existing.setFpermission(permission.name());
            existing.setFmodifytime(now);
            aclMapper.updateById(existing);
        }
        return toResponse(existing);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean revoke(String kbId, Long aclId) {
        String normalizedKbId = requireBaseId(kbId);
        if (aclId == null || aclId <= 0) {
            throw new BizException("ACL记录编号无效");
        }
        BizfiAiKnowledgeBaseAcl entry = aclMapper.selectOne(new LambdaQueryWrapper<BizfiAiKnowledgeBaseAcl>()
                .eq(BizfiAiKnowledgeBaseAcl::getFid, aclId)
                .eq(BizfiAiKnowledgeBaseAcl::getFkbid, normalizedKbId)
                .last("limit 1"));
        if (entry == null) {
            throw new BizException("ACL记录不存在");
        }
        AiKnowledgePermission permission = parseStoredPermission(entry.getFpermission());
        assertCanManageAcl(
                normalizedKbId,
                permission == AiKnowledgePermission.OWNER
                        ? AiKnowledgePermission.OWNER
                        : AiKnowledgePermission.ADMIN
        );
        if (permission == AiKnowledgePermission.OWNER) {
            assertAnotherOwnerExists(normalizedKbId, entry.getFid());
        }
        return aclMapper.deleteById(entry.getFid()) > 0;
    }

    @Transactional(rollbackFor = Exception.class)
    public void bootstrapOwner(String kbId) {
        String normalizedKbId = requireBaseId(kbId);
        Long userId = requireCurrentUserId();
        BizfiAiKnowledgeBaseAcl existing = aclMapper.selectOne(new LambdaQueryWrapper<BizfiAiKnowledgeBaseAcl>()
                .eq(BizfiAiKnowledgeBaseAcl::getFkbid, normalizedKbId)
                .eq(BizfiAiKnowledgeBaseAcl::getFsubjecttype, AiKnowledgeAclSubjectType.USER.name())
                .eq(BizfiAiKnowledgeBaseAcl::getFsubjectid, String.valueOf(userId))
                .last("limit 1"));
        if (existing != null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        BizfiAiKnowledgeBaseAcl owner = new BizfiAiKnowledgeBaseAcl();
        owner.setFkbid(normalizedKbId);
        owner.setFsubjecttype(AiKnowledgeAclSubjectType.USER.name());
        owner.setFsubjectid(String.valueOf(userId));
        owner.setFpermission(AiKnowledgePermission.OWNER.name());
        owner.setFcreatedby(userId);
        owner.setFcreatetime(now);
        owner.setFmodifytime(now);
        aclMapper.insert(owner);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteEntries(String kbId) {
        aclMapper.delete(new LambdaQueryWrapper<BizfiAiKnowledgeBaseAcl>()
                .eq(BizfiAiKnowledgeBaseAcl::getFkbid, normalizeKbId(kbId)));
    }

    private void assertPermission(String kbId, AiKnowledgePermission required) {
        PrincipalSubjects principal = currentPrincipal();
        if (!isEnabled() || isSystemAdmin(principal)) {
            return;
        }
        AiKnowledgePermission actual = principal.userId() == null
                ? null
                : highestPermission(loadMatchingEntries(principal, normalizeKbId(kbId)));
        if (actual == null || !actual.allows(required)) {
            throw new BizException(
                    "当前用户无权访问知识库 " + normalizeKbId(kbId)
                            + "，至少需要 " + required.name() + " 权限"
            );
        }
    }

    private void assertCanManageAcl(String kbId, AiKnowledgePermission required) {
        PrincipalSubjects principal = currentPrincipal();
        if (isSystemAdmin(principal)) {
            return;
        }
        if (principal.userId() == null) {
            throw new BizException("未获取到当前登录用户");
        }
        AiKnowledgePermission actual = highestPermission(
                loadMatchingEntries(principal, normalizeKbId(kbId))
        );
        if (actual == null || !actual.allows(required)) {
            throw new BizException("当前用户无权管理知识库ACL，至少需要 " + required.name() + " 权限");
        }
    }

    private List<BizfiAiKnowledgeBaseAcl> loadMatchingEntries(
            PrincipalSubjects principal,
            String kbId
    ) {
        return aclMapper.selectMatching(
                String.valueOf(principal.userId()),
                principal.organizationIds(),
                principal.authorities(),
                StringUtils.hasText(kbId) ? kbId : null
        );
    }

    private AiKnowledgePermission highestPermission(List<BizfiAiKnowledgeBaseAcl> entries) {
        if (entries == null || entries.isEmpty()) {
            return null;
        }
        return entries.stream()
                .map(entry -> parseStoredPermission(entry.getFpermission()))
                .filter(Objects::nonNull)
                .max(Comparator.comparingInt(AiKnowledgePermission::ordinal))
                .orElse(null);
    }

    private boolean permissionAllows(String storedPermission, AiKnowledgePermission required) {
        AiKnowledgePermission permission = parseStoredPermission(storedPermission);
        return permission != null && permission.allows(required);
    }

    private boolean isSystemAdmin(PrincipalSubjects principal) {
        if (principal.userId() != null
                && configuredValues(properties.getAdminUserIds())
                .contains(String.valueOf(principal.userId()))) {
            return true;
        }
        Set<String> configuredAuthorities = configuredValues(properties.getAdminAuthorities()).stream()
                .map(this::normalizeAuthority)
                .collect(Collectors.toSet());
        return principal.authorities().stream().anyMatch(configuredAuthorities::contains);
    }

    private PrincipalSubjects currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = null;
        if (authentication != null && authentication.getPrincipal() != null) {
            try {
                userId = Long.valueOf(authentication.getPrincipal().toString());
            } catch (NumberFormatException ignored) {
                userId = null;
            }
        }
        Set<String> authorities = authentication == null || authentication.getAuthorities() == null
                ? Set.of()
                : authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(StringUtils::hasText)
                .map(this::normalizeAuthority)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> organizations = authorities.stream()
                .map(this::extractOrganizationId)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return new PrincipalSubjects(userId, authorities, organizations);
    }

    private String extractOrganizationId(String authority) {
        if (!StringUtils.hasText(authority)) {
            return null;
        }
        String value = normalizeAuthority(authority);
        for (String prefix : List.of("org:", "org_", "organization:", "organization_")) {
            if (value.startsWith(prefix) && value.length() > prefix.length()) {
                return value.substring(prefix.length());
            }
        }
        return null;
    }

    private String normalizeSubjectId(AiKnowledgeAclSubjectType type, String value) {
        if (!StringUtils.hasText(value)) {
            throw new BizException("ACL主体编号不能为空");
        }
        String normalized = value.trim();
        if (type == AiKnowledgeAclSubjectType.USER) {
            try {
                if (Long.parseLong(normalized) <= 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException failure) {
                throw new BizException("用户主体编号必须为正整数");
            }
        }
        if (type == AiKnowledgeAclSubjectType.AUTHORITY) {
            normalized = normalizeAuthority(normalized);
        } else if (type == AiKnowledgeAclSubjectType.ORGANIZATION) {
            normalized = normalized.toLowerCase(Locale.ROOT);
        }
        return normalized.length() <= 128 ? normalized : normalized.substring(0, 128);
    }

    private String requireBaseId(String kbId) {
        String normalized = normalizeKbId(kbId);
        BizfiAiKnowledgeBase base = baseMapper.selectOne(new LambdaQueryWrapper<BizfiAiKnowledgeBase>()
                .eq(BizfiAiKnowledgeBase::getFkbid, normalized)
                .last("limit 1"));
        if (base == null) {
            throw new BizException("知识库不存在");
        }
        return normalized;
    }

    private Long requireCurrentUserId() {
        Long userId = currentPrincipal().userId();
        if (userId == null || userId <= 0) {
            throw new BizException("未获取到当前登录用户");
        }
        return userId;
    }

    private void assertAnotherOwnerExists(String kbId, Long excludedId) {
        long owners = aclMapper.selectList(new LambdaQueryWrapper<BizfiAiKnowledgeBaseAcl>()
                        .eq(BizfiAiKnowledgeBaseAcl::getFkbid, kbId)
                        .eq(BizfiAiKnowledgeBaseAcl::getFpermission, AiKnowledgePermission.OWNER.name()))
                .stream()
                .filter(entry -> excludedId == null || !excludedId.equals(entry.getFid()))
                .count();
        if (owners <= 0) {
            throw new BizException("知识库必须至少保留一个OWNER");
        }
    }

    private AiKnowledgePermission parseStoredPermission(String permission) {
        try {
            return AiKnowledgePermission.parse(permission);
        } catch (BizException ignored) {
            return null;
        }
    }

    private AiKnowledgeAclResponse toResponse(BizfiAiKnowledgeBaseAcl entry) {
        return new AiKnowledgeAclResponse(
                entry.getFid(),
                entry.getFkbid(),
                entry.getFsubjecttype(),
                entry.getFsubjectid(),
                entry.getFpermission(),
                entry.getFcreatedby(),
                entry.getFcreatetime(),
                entry.getFmodifytime()
        );
    }

    private Set<String> configuredValues(String configured) {
        if (!StringUtils.hasText(configured)) {
            return Set.of();
        }
        return Arrays.stream(configured.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toUnmodifiableSet());
    }

    private String normalizeAuthority(String authority) {
        return authority == null ? "" : authority.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeKbId(String kbId) {
        if (!StringUtils.hasText(kbId)) {
            return AiKnowledgeBaseServiceImpl.DEFAULT_KB_ID;
        }
        String normalized = kbId.trim()
                .replaceAll("[^A-Za-z0-9_-]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_+|_+$", "")
                .toLowerCase(Locale.ROOT);
        return normalized.length() <= 64 ? normalized : normalized.substring(0, 64);
    }

    private record PrincipalSubjects(
            Long userId,
            Set<String> authorities,
            Set<String> organizationIds
    ) {
    }
}
