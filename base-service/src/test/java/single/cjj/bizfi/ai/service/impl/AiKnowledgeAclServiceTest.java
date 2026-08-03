package single.cjj.bizfi.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import single.cjj.bizfi.ai.config.KnowledgeAclProperties;
import single.cjj.bizfi.ai.entity.BizfiAiKnowledgeBase;
import single.cjj.bizfi.ai.entity.BizfiAiKnowledgeBaseAcl;
import single.cjj.bizfi.ai.mapper.BizfiAiKnowledgeBaseAclMapper;
import single.cjj.bizfi.ai.mapper.BizfiAiKnowledgeBaseMapper;
import single.cjj.bizfi.ai.security.AiKnowledgePermission;
import single.cjj.bizfi.exception.BizException;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiKnowledgeAclServiceTest {

    @Mock
    private BizfiAiKnowledgeBaseAclMapper aclMapper;

    @Mock
    private BizfiAiKnowledgeBaseMapper baseMapper;

    private KnowledgeAclProperties properties;
    private AiKnowledgeAclService service;

    @BeforeEach
    void setUp() {
        properties = new KnowledgeAclProperties();
        properties.setEnabled(true);
        properties.setAdminUserIds("");
        properties.setAdminAuthorities("ROLE_ADMIN");
        service = new AiKnowledgeAclService(aclMapper, baseMapper, properties);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldRemainUnrestrictedWhileFeatureIsDisabled() {
        properties.setEnabled(false);

        assertNull(service.resolveAccessibleBaseIds(AiKnowledgePermission.VIEWER));
        assertEquals(AiKnowledgePermission.OWNER, service.effectivePermission("finance"));
        verify(aclMapper, never()).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    void shouldResolveDirectUserPermission() {
        authenticate("1001");
        when(aclMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(acl("finance", "USER", "1001", "VIEWER")));

        assertEquals(Set.of("finance"), service.resolveAccessibleBaseIds(AiKnowledgePermission.VIEWER));
        assertEquals(Set.of(), service.resolveAccessibleBaseIds(AiKnowledgePermission.EDITOR));
    }

    @Test
    void shouldResolveOrganizationPermissionFromJwtAuthority() {
        authenticate("1001", "org:88");
        when(aclMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(acl("finance", "ORGANIZATION", "88", "EDITOR")));

        assertEquals(AiKnowledgePermission.EDITOR, service.effectivePermission("finance"));
        assertDoesNotThrow(() -> service.assertCanEdit("finance"));
    }

    @Test
    void shouldRejectWhenPermissionIsInsufficient() {
        authenticate("1001");
        when(aclMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(acl("finance", "USER", "1001", "VIEWER")));

        assertThrows(BizException.class, () -> service.assertCanEdit("finance"));
    }

    @Test
    void shouldAllowConfiguredAdministratorAuthority() {
        authenticate("1001", "ROLE_ADMIN");

        assertNull(service.resolveAccessibleBaseIds(AiKnowledgePermission.OWNER));
        assertEquals(AiKnowledgePermission.OWNER, service.effectivePermission("private"));
        verify(aclMapper, never()).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    void shouldBootstrapCreatorAsOwner() {
        authenticate("1001");
        BizfiAiKnowledgeBase base = new BizfiAiKnowledgeBase();
        base.setFkbid("finance");
        when(baseMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(base);
        when(aclMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        service.bootstrapOwner("finance");

        verify(aclMapper).insert(any(BizfiAiKnowledgeBaseAcl.class));
    }

    private void authenticate(String userId, String... authorities) {
        List<SimpleGrantedAuthority> granted = java.util.Arrays.stream(authorities)
                .map(SimpleGrantedAuthority::new)
                .toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, granted)
        );
    }

    private BizfiAiKnowledgeBaseAcl acl(
            String kbId,
            String subjectType,
            String subjectId,
            String permission
    ) {
        BizfiAiKnowledgeBaseAcl entry = new BizfiAiKnowledgeBaseAcl();
        entry.setFkbid(kbId);
        entry.setFsubjecttype(subjectType);
        entry.setFsubjectid(subjectId);
        entry.setFpermission(permission);
        return entry;
    }
}
