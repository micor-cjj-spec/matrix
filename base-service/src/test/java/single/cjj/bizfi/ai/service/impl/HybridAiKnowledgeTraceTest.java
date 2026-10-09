package single.cjj.bizfi.ai.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import single.cjj.bizfi.ai.config.AiProperties;
import single.cjj.bizfi.ai.config.AiVectorStoreProperties;
import single.cjj.bizfi.ai.dto.AiCitationResponse;
import single.cjj.bizfi.ai.dto.AiKnowledgeRetrievalResponse;
import single.cjj.bizfi.ai.service.AiKnowledgeService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HybridAiKnowledgeTraceTest {

    @Mock
    private AiKnowledgeService keywordRetriever;

    @Mock
    private AiEmbeddingClient embeddingClient;

    @Mock
    private LegacyJsonSemanticRetriever legacyJsonRetriever;

    @Mock
    private ObjectProvider<PgVectorSemanticRetriever> pgVectorRetrieverProvider;

    @Mock
    private PgVectorSemanticRetriever pgVectorRetriever;

    private AiProperties properties;
    private AiVectorStoreProperties vectorStoreProperties;
    private HybridAiKnowledgeService service;

    @BeforeEach
    void setUp() {
        properties = new AiProperties();
        properties.setSemanticRetrievalEnabled(true);
        properties.setSemanticFailOpen(true);
        properties.setHybridKeywordWeight(1.0D);
        properties.setHybridSemanticWeight(2.0D);
        properties.setHybridRrfK(60);
        vectorStoreProperties = new AiVectorStoreProperties();
        service = new HybridAiKnowledgeService(
                keywordRetriever,
                embeddingClient,
                legacyJsonRetriever,
                pgVectorRetrieverProvider,
                properties,
                vectorStoreProperties
        );
    }

    @Test
    void shouldExposeRrfCandidatesAndConfigurationFingerprint() {
        AiCitationResponse keyword = citation(
                "doc_keyword",
                "关键词文档",
                "chunk_keyword",
                "凭证过账规则"
        );
        AiCitationResponse semantic = citation(
                "doc_semantic",
                "月结制度",
                "chunk_semantic",
                "月结前应完成凭证过账和损益结转。"
        );
        when(keywordRetriever.retrieve(eq("月结前要做什么"), eq(List.of("all")), any()))
                .thenReturn(List.of(keyword));
        when(embeddingClient.embed(List.of("月结前要做什么")))
                .thenReturn(embedding());
        when(legacyJsonRetriever.retrieve(
                List.of(1.0D, 0.0D),
                "embedding-test",
                List.of("all"),
                8
        )).thenReturn(List.of(semantic));

        AiKnowledgeRetrievalResponse response = service.retrieveWithTrace(
                "月结前要做什么",
                List.of("all"),
                2
        );

        assertEquals("HYBRID_RRF", response.trace().mode());
        assertEquals(AiVectorStoreProperties.MYSQL_JSON, response.trace().actualSemanticBackend());
        assertEquals(1, response.trace().keywordCandidates().size());
        assertEquals(1, response.trace().semanticCandidates().size());
        assertEquals(2, response.trace().fusedCandidates().size());
        assertEquals("chunk_semantic", response.citations().get(0).getChunkId());
        assertTrue(response.trace().semanticCandidates().get(0).rrfContribution()
                > response.trace().keywordCandidates().get(0).rrfContribution());
        assertNotNull(response.trace().configFingerprint());
        assertEquals(16, response.trace().configFingerprint().length());
    }

    @Test
    void shouldReportPgVectorFallbackInTrace() {
        vectorStoreProperties.setType(AiVectorStoreProperties.PGVECTOR);
        vectorStoreProperties.setReadFallbackEnabled(true);
        AiCitationResponse fallback = citation(
                "doc_legacy",
                "旧向量",
                "chunk_legacy",
                "MySQL JSON 回退结果"
        );
        when(keywordRetriever.retrieve(eq("付款规则"), eq(List.of()), any()))
                .thenReturn(List.of());
        when(embeddingClient.embed(List.of("付款规则"))).thenReturn(embedding());
        when(pgVectorRetrieverProvider.getIfAvailable()).thenReturn(pgVectorRetriever);
        when(pgVectorRetriever.retrieve(any(), any(), any(), any(Integer.class)))
                .thenThrow(new IllegalStateException("pgvector unavailable"));
        when(legacyJsonRetriever.retrieve(
                List.of(1.0D, 0.0D),
                "embedding-test",
                List.of(),
                20
        )).thenReturn(List.of(fallback));

        AiKnowledgeRetrievalResponse response = service.retrieveWithTrace(
                "付款规则",
                List.of(),
                5
        );

        assertEquals("HYBRID_RRF", response.trace().mode());
        assertEquals(AiVectorStoreProperties.PGVECTOR, response.trace().configuredVectorStore());
        assertEquals(AiVectorStoreProperties.MYSQL_JSON, response.trace().actualSemanticBackend());
        assertTrue(response.trace().fallbackUsed());
        assertTrue(response.trace().fallbackReason().contains("pgvector unavailable"));
        assertFalse(response.citations().isEmpty());
    }

    private AiEmbeddingClient.EmbeddingBatch embedding() {
        return new AiEmbeddingClient.EmbeddingBatch(
                "embedding-test",
                2,
                List.of(List.of(1.0D, 0.0D))
        );
    }

    private AiCitationResponse citation(String docId, String docName, String chunkId, String snippet) {
        return new AiCitationResponse(docId, docName, chunkId, snippet);
    }
}
