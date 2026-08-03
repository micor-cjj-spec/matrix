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

    private AiKnowledgeScopeResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new AiKnowledgeScopeResolver(knowledgeBaseMapper, knowledgeDocMapper);
    }

    @Test
    void shouldTreatAllAliasAsUnrestricted() {
        assertNull(resolver.resolveAllowedDocumentIds(List.of("all")));
        verify(knowledgeBaseMapper, never()).selectList(any());
        verify(knowledgeDocMapper, never()).selectList(any());
    }

    @Test
    void shouldResolveActiveKnowledgeBaseAndLegacyDocumentTogether() {
        BizfiAiKnowledgeBase finance = knowledgeBase("finance", "ACTIVE");
        BizfiAiKnowledgeDoc closeGuide = document("finance", "month_end_close");
        BizfiAiKnowledgeDoc voucherGuide = document("finance", "voucher_review");
        when(knowledgeBaseMapper.selectList(any())).thenReturn(List.of(finance));
        when(knowledgeDocMapper.selectList(any())).thenReturn(List.of(closeGuide, voucherGuide));

        Set<String> result = resolver.resolveAllowedDocumentIds(List.of("finance", "legacy_doc"));

        assertEquals(Set.of("month_end_close", "voucher_review", "legacy_doc"), result);
    }

    @Test
    void shouldNotTreatInactiveKnowledgeBaseAsLegacyDocument() {
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
