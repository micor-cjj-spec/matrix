package single.cjj.bizfi.ai.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import single.cjj.bizfi.ai.entity.BizfiAiKnowledgeBase;
import single.cjj.bizfi.ai.entity.BizfiAiKnowledgeDoc;
import single.cjj.bizfi.ai.mapper.BizfiAiKnowledgeBaseMapper;
import single.cjj.bizfi.ai.mapper.BizfiAiKnowledgeDocMapper;
import single.cjj.bizfi.ai.security.AiKnowledgePermission;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiKnowledgeScopeResolverTest {

    @Mock
    private BizfiAiKnowledgeBaseMapper knowledgeBaseMapper;

    @Mock
    private BizfiAiKnowledgeDocMapper knowledgeDocMapper;

    @Mock
    private AiKnowledgeAclService aclService;

    private AiKnowledgeScopeResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new AiKnowledgeScopeResolver(knowledgeBaseMapper, knowledgeDocMapper, aclService);
    }

    @Test
    void shouldTreatAllAliasAsUnrestrictedWhenAclIsUnrestricted() {
        when(aclService.resolveAccessibleBaseIds(AiKnowledgePermission.VIEWER)).thenReturn(null);

        assertNull(resolver.resolveAllowedDocumentIds(List.of("all")));
        verify(knowledgeBaseMapper, never()).selectList(any());
        verify(knowledgeDocMapper, never()).selectList(any());
    }

    @Test
    void shouldResolveOnlyAccessibleBasesForGlobalScope() {
        when(aclService.resolveAccessibleBaseIds(AiKnowledgePermission.VIEWER)).thenReturn(Set.of("finance"));
        when(knowledgeDocMapper.selectList(any())).thenReturn(List.of(document("finance", "month_end_close")));

        Set<String> result = resolver.resolveAllowedDocumentIds(List.of("all"));

        assertEquals(Set.of("month_end_close"), result);
        verify(knowledgeBaseMapper, never()).selectList(any());
    }

    @Test
    void shouldResolveActiveKnowledgeBaseAndAuthorizedLegacyDocumentTogether() {
        BizfiAiKnowledgeBase finance = knowledgeBase("finance", "ACTIVE");
        BizfiAiKnowledgeDoc closeGuide = document("finance", "month_end_close");
        BizfiAiKnowledgeDoc legacyGuide = document("finance", "legacy_doc");
        when(aclService.resolveAccessibleBaseIds(AiKnowledgePermission.VIEWER)).thenReturn(Set.of("finance"));
        when(knowledgeBaseMapper.selectList(any())).thenReturn(List.of(finance));
        when(knowledgeDocMapper.selectList(any()))
                .thenReturn(List.of(closeGuide))
                .thenReturn(List.of(legacyGuide));

        Set<String> result = resolver.resolveAllowedDocumentIds(List.of("finance", "legacy_doc"));

        assertEquals(Set.of("month_end_close", "legacy_doc"), result);
    }

    @Test
    void shouldRejectLegacyDocumentFromInaccessibleBase() {
        when(aclService.resolveAccessibleBaseIds(AiKnowledgePermission.VIEWER)).thenReturn(Set.of("finance"));
        when(knowledgeBaseMapper.selectList(any())).thenReturn(List.of());
        when(knowledgeDocMapper.selectList(any())).thenReturn(List.of(document("hr", "private_hr_doc")));

        Set<String> result = resolver.resolveAllowedDocumentIds(List.of("private_hr_doc"));

        assertEquals(Set.of(), result);
    }

    @Test
    void shouldNotTreatInactiveKnowledgeBaseAsLegacyDocument() {
        when(aclService.resolveAccessibleBaseIds(AiKnowledgePermission.VIEWER)).thenReturn(null);
        when(knowledgeBaseMapper.selectList(any())).thenReturn(List.of(knowledgeBase("finance", "INACTIVE")));

        Set<String> result = resolver.resolveAllowedDocumentIds(List.of("finance"));

        assertEquals(Set.of(), result);
        verify(knowledgeDocMapper, never()).selectList(any());
    }

    private BizfiAiKnowledgeBase knowledgeBase(String kbId, String status) {
        BizfiAiKnowledgeBase base = new BizfiAiKnowledgeBase();
        base.setFkbid(kbId);
        base.setFstatus(status);
        return base;
    }

    private BizfiAiKnowledgeDoc document(String kbId, String docId) {
        BizfiAiKnowledgeDoc doc = new BizfiAiKnowledgeDoc();
        doc.setFkbid(kbId);
        doc.setFdocid(docId);
        doc.setFstatus("ACTIVE");
        return doc;
    }
}
