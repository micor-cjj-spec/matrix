package single.cjj.bizfi.ai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import single.cjj.bizfi.ai.config.AiProperties;
import single.cjj.bizfi.ai.dto.AiCitationResponse;
import single.cjj.bizfi.ai.entity.BizfiAiKnowledgeChunk;
import single.cjj.bizfi.ai.entity.BizfiAiKnowledgeDoc;
import single.cjj.bizfi.ai.mapper.BizfiAiKnowledgeChunkMapper;
import single.cjj.bizfi.ai.mapper.BizfiAiKnowledgeDocMapper;
import single.cjj.bizfi.ai.service.AiKnowledgeService;

import java.time.LocalDateTime;
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
    private BizfiAiKnowledgeChunkMapper chunkMapper;

    @Mock
    private BizfiAiKnowledgeDocMapper docMapper;

    private AiProperties properties;
    private HybridAiKnowledgeService service;

    @BeforeEach
    void setUp() {
        properties = new AiProperties();
        properties.setSemanticRetrievalEnabled(true);
        properties.setSemanticFailOpen(true);
        properties.setSemanticMinScore(0.1D);
        properties.setHybridKeywordWeight(1.0D);
        properties.setHybridSemanticWeight(2.0D);
        service = new HybridAiKnowledgeService(
                keywordRetriever,
                embeddingClient,
                chunkMapper,
                docMapper,
                new ObjectMapper(),
                properties
        );
    }

    @Test
    void shouldFuseKeywordAndSemanticResults() {
        AiCitationResponse keyword = new AiCitationResponse(
                "doc_keyword",
                "关键词文档",
                "chunk_keyword",
                "凭证过账规则"
        );
        when(keywordRetriever.retrieve(eq("关账前需要做什么"), eq(List.of("all")), any()))
                .thenReturn(List.of(keyword));
        when(embeddingClient.embed(List.of("关账前需要做什么")))
                .thenReturn(new AiEmbeddingClient.EmbeddingBatch(
                        "embedding-test",
                        2,
                        List.of(List.of(1.0D, 0.0D))
                ));

        BizfiAiKnowledgeChunk semanticChunk = new BizfiAiKnowledgeChunk();
        semanticChunk.setFid(2L);
        semanticChunk.setFdocid("doc_semantic");
        semanticChunk.setFchunkid("chunk_semantic");
        semanticChunk.setFcontent("期末结账前应完成凭证过账、损益结转和一致性检查。");
        semanticChunk.setFembedding("[1.0,0.0]");
        semanticChunk.setFembeddingmodel("embedding-test");
        semanticChunk.setFembeddingdimensions(2);
        semanticChunk.setFembeddingtime(LocalDateTime.now());
        when(chunkMapper.selectList(any())).thenReturn(List.of(semanticChunk));

        BizfiAiKnowledgeDoc semanticDoc = new BizfiAiKnowledgeDoc();
        semanticDoc.setFdocid("doc_semantic");
        semanticDoc.setFtitle("月结制度");
        semanticDoc.setFstatus("ACTIVE");
        when(docMapper.selectList(any())).thenReturn(List.of(semanticDoc));

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
    void shouldFallBackToKeywordResultsWhenEmbeddingFails() {
        AiCitationResponse keyword = new AiCitationResponse(
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
}
