package single.cjj.bizfi.ai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import single.cjj.bizfi.ai.config.AiProperties;
import single.cjj.bizfi.ai.entity.BizfiAiKnowledgeChunk;
import single.cjj.bizfi.ai.mapper.BizfiAiKnowledgeChunkMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiKnowledgeVectorIndexServiceTest {

    @Mock
    private BizfiAiKnowledgeChunkMapper chunkMapper;

    @Mock
    private AiEmbeddingClient embeddingClient;

    private AiProperties properties;
    private AiKnowledgeVectorIndexService service;

    @BeforeEach
    void setUp() {
        properties = new AiProperties();
        properties.setSemanticRetrievalEnabled(true);
        properties.setSemanticFailOpen(false);
        properties.setEmbeddingBatchSize(2);
        service = new AiKnowledgeVectorIndexService(
                chunkMapper,
                embeddingClient,
                new ObjectMapper(),
                properties
        );
    }

    @Test
    void shouldPersistEmbeddingMetadataForEveryChunk() {
        BizfiAiKnowledgeChunk first = chunk(1L, "doc_1_1", "第一段知识");
        BizfiAiKnowledgeChunk second = chunk(2L, "doc_1_2", "第二段知识");
        when(chunkMapper.selectList(any())).thenReturn(List.of(first, second));
        when(embeddingClient.embed(List.of("第一段知识", "第二段知识")))
                .thenReturn(new AiEmbeddingClient.EmbeddingBatch(
                        "embedding-test",
                        3,
                        List.of(
                                List.of(1.0D, 0.0D, 0.5D),
                                List.of(0.0D, 1.0D, 0.5D)
                        )
                ));

        AiKnowledgeVectorIndexService.IndexResult result = service.reindexDocument("doc_1");

        assertEquals("INDEXED", result.status());
        assertEquals(2, result.indexedChunks());
        assertEquals("embedding-test", result.model());
        assertEquals(3, result.dimensions());

        ArgumentCaptor<BizfiAiKnowledgeChunk> captor = ArgumentCaptor.forClass(BizfiAiKnowledgeChunk.class);
        verify(chunkMapper, times(2)).updateById(captor.capture());
        for (BizfiAiKnowledgeChunk updated : captor.getAllValues()) {
            assertEquals("embedding-test", updated.getFembeddingmodel());
            assertEquals(3, updated.getFembeddingdimensions());
            assertNotNull(updated.getFembedding());
            assertNotNull(updated.getFembeddingtime());
        }
    }

    @Test
    void shouldSkipAutomaticIndexingWhenFeatureIsDisabled() {
        properties.setSemanticRetrievalEnabled(false);

        AiKnowledgeVectorIndexService.IndexResult result = service.indexDocumentIfEnabled("doc_1");

        assertEquals("DISABLED", result.status());
        assertEquals(0, result.indexedChunks());
    }

    private BizfiAiKnowledgeChunk chunk(Long id, String chunkId, String content) {
        BizfiAiKnowledgeChunk chunk = new BizfiAiKnowledgeChunk();
        chunk.setFid(id);
        chunk.setFdocid("doc_1");
        chunk.setFchunkid(chunkId);
        chunk.setFcontent(content);
        return chunk;
    }
}
