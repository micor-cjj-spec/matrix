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
import single.cjj.bizfi.ai.service.AiKnowledgeService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HybridAiKnowledgeServiceTest {

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
    void shouldFuseKeywordAndLegacySemanticResults() {
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
                "期末结账前应完成凭证过账、损益结转和一致性检查。"
        );
        when(keywordRetriever.retrieve(eq("关账前需要做什么"), eq(List.of("all")), any()))
                .thenReturn(List.of(keyword));
        when(embeddingClient.embed(List.of("关账前需要做什么")))
                .thenReturn(embedding());
        when(legacyJsonRetriever.retrieve(
                List.of(1.0D, 0.0D),
                "embedding-test",
                List.of("all"),
                8
        )).thenReturn(List.of(semantic));

        List<AiCitationResponse> results = service.retrieve(
                "关账前需要做什么",
                List.of("all"),
                2
        );

        assertEquals(2, results.size());
        assertEquals("chunk_semantic", results.get(0).getChunkId());
        assertTrue(results.stream().anyMatch(item -> "chunk_keyword".equals(item.getChunkId())));
    }

    @Test
    void shouldRouteSemanticSearchToPgVector() {
        vectorStoreProperties.setType(AiVectorStoreProperties.PGVECTOR);
        AiCitationResponse semantic = citation(
                "doc_pg",
                "PGVector知识",
                "chunk_pg",
                "PGVector召回结果"
        );
        when(keywordRetriever.retrieve(eq("月结规则"), eq(List.of()), any()))
                .thenReturn(List.of());
        when(embeddingClient.embed(List.of("月结规则"))).thenReturn(embedding());
        when(pgVectorRetrieverProvider.getIfAvailable()).thenReturn(pgVectorRetriever);
        when(pgVectorRetriever.retrieve(
                List.of(1.0D, 0.0D),
                "embedding-test",
                List.of(),
                20
        )).thenReturn(List.of(semantic));

        List<AiCitationResponse> results = service.retrieve("月结规则", List.of(), 5);

        assertEquals(List.of(semantic), results);
    }

    @Test
    void shouldFallBackToLegacySemanticSearchWhenPgVectorFails() {
        vectorStoreProperties.setType(AiVectorStoreProperties.PGVECTOR);
        vectorStoreProperties.setReadFallbackEnabled(true);
        AiCitationResponse fallback = citation(
                "doc_legacy",
                "旧向量",
                "chunk_legacy",
                "MySQL JSON回退结果"
        );
        when(keywordRetriever.retrieve(eq("月结规则"), eq(List.of()), any()))
                .thenReturn(List.of());
        when(embeddingClient.embed(List.of("月结规则"))).thenReturn(embedding());
        when(pgVectorRetrieverProvider.getIfAvailable()).thenReturn(pgVectorRetriever);
        when(pgVectorRetriever.retrieve(any(), any(), any(), any(Integer.class)))
                .thenThrow(new IllegalStateException("pgvector unavailable"));
        when(legacyJsonRetriever.retrieve(
                List.of(1.0D, 0.0D),
                "embedding-test",
                List.of(),
                20
        )).thenReturn(List.of(fallback));

        List<AiCitationResponse> results = service.retrieve("月结规则", List.of(), 5);

        assertEquals(List.of(fallback), results);
    }

    @Test
    void shouldFallBackToKeywordResultsWhenEmbeddingFails() {
        AiCitationResponse keyword = citation(
                "doc_keyword",
                "关键词文档",
                "chunk_keyword",
                "月结规则"
        );
        when(keywordRetriever.retrieve(eq("月结规则"), eq(List.of()), any()))
                .thenReturn(List.of(keyword));
        when(embeddingClient.embed(List.of("月结规则")))
                .thenThrow(new IllegalStateException("embedding unavailable"));

        List<AiCitationResponse> results = service.retrieve("月结规则", List.of(), 5);

        assertEquals(List.of(keyword), results);
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
