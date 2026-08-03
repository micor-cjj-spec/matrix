package single.cjj.bizfi.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.ai.config.AiProperties;
import single.cjj.bizfi.ai.entity.BizfiAiKnowledgeChunk;
import single.cjj.bizfi.ai.mapper.BizfiAiKnowledgeChunkMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class AiKnowledgeVectorIndexService {

    private static final Logger log = LoggerFactory.getLogger(AiKnowledgeVectorIndexService.class);

    private final BizfiAiKnowledgeChunkMapper chunkMapper;
    private final AiEmbeddingClient embeddingClient;
    private final ObjectMapper objectMapper;
    private final AiProperties properties;

    public AiKnowledgeVectorIndexService(
            BizfiAiKnowledgeChunkMapper chunkMapper,
            AiEmbeddingClient embeddingClient,
            ObjectMapper objectMapper,
            AiProperties properties
    ) {
        this.chunkMapper = chunkMapper;
        this.embeddingClient = embeddingClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public boolean isEnabled() {
        return Boolean.TRUE.equals(properties.getSemanticRetrievalEnabled());
    }

    public IndexResult indexDocumentIfEnabled(String docId) {
        if (!isEnabled()) {
            return new IndexResult(docId, 0, null, 0, "DISABLED");
        }
        try {
            return reindexDocument(docId);
        } catch (RuntimeException failure) {
            if (Boolean.TRUE.equals(properties.getSemanticFailOpen())) {
                log.warn("Knowledge embedding indexing failed, keyword retrieval remains available. docId={}", docId, failure);
                return new IndexResult(docId, 0, null, 0, "FAILED");
            }
            throw failure;
        }
    }

    public IndexResult reindexDocument(String docId) {
        requireEnabled();
        if (!StringUtils.hasText(docId)) {
            throw new IllegalArgumentException("docId 不能为空");
        }

        List<BizfiAiKnowledgeChunk> chunks = chunkMapper.selectList(
                new LambdaQueryWrapper<BizfiAiKnowledgeChunk>()
                        .eq(BizfiAiKnowledgeChunk::getFdocid, docId.trim())
                        .orderByAsc(BizfiAiKnowledgeChunk::getFseq)
        );
        if (chunks == null || chunks.isEmpty()) {
            return new IndexResult(docId.trim(), 0, null, 0, "EMPTY");
        }

        int batchSize = resolveBatchSize();
        int indexed = 0;
        String model = null;
        Integer dimensions = null;
        for (int start = 0; start < chunks.size(); start += batchSize) {
            int end = Math.min(chunks.size(), start + batchSize);
            List<BizfiAiKnowledgeChunk> batchChunks = chunks.subList(start, end);
            List<String> texts = batchChunks.stream()
                    .map(BizfiAiKnowledgeChunk::getFcontent)
                    .toList();
            AiEmbeddingClient.EmbeddingBatch batch = embeddingClient.embed(texts);
            validateBatch(batch, batchChunks.size());

            model = batch.model();
            dimensions = batch.dimensions();
            LocalDateTime indexedAt = LocalDateTime.now();
            for (int offset = 0; offset < batchChunks.size(); offset++) {
                BizfiAiKnowledgeChunk chunk = batchChunks.get(offset);
                chunk.setFembedding(toJson(batch.vectors().get(offset)));
                chunk.setFembeddingmodel(model);
                chunk.setFembeddingdimensions(dimensions);
                chunk.setFembeddingtime(indexedAt);
                chunkMapper.updateById(chunk);
                indexed++;
            }
        }

        return new IndexResult(docId.trim(), indexed, model, dimensions == null ? 0 : dimensions, "INDEXED");
    }

    public BulkIndexResult reindexAll(boolean onlyMissing) {
        requireEnabled();
        LambdaQueryWrapper<BizfiAiKnowledgeChunk> wrapper = new LambdaQueryWrapper<>();
        if (onlyMissing) {
            wrapper.and(query -> query.isNull(BizfiAiKnowledgeChunk::getFembedding)
                    .or()
                    .eq(BizfiAiKnowledgeChunk::getFembedding, ""));
        }
        wrapper.orderByAsc(BizfiAiKnowledgeChunk::getFdocid)
                .orderByAsc(BizfiAiKnowledgeChunk::getFseq);

        List<BizfiAiKnowledgeChunk> chunks = chunkMapper.selectList(wrapper);
        Set<String> docIds = new LinkedHashSet<>();
        if (chunks != null) {
            chunks.stream()
                    .map(BizfiAiKnowledgeChunk::getFdocid)
                    .filter(StringUtils::hasText)
                    .forEach(docIds::add);
        }

        int indexedDocuments = 0;
        int indexedChunks = 0;
        List<String> failedDocIds = new ArrayList<>();
        for (String docId : docIds) {
            try {
                IndexResult result = reindexDocument(docId);
                indexedDocuments++;
                indexedChunks += result.indexedChunks();
            } catch (RuntimeException failure) {
                failedDocIds.add(docId);
                if (!Boolean.TRUE.equals(properties.getSemanticFailOpen())) {
                    throw failure;
                }
                log.warn("Bulk knowledge embedding indexing failed. docId={}", docId, failure);
            }
        }
        return new BulkIndexResult(docIds.size(), indexedDocuments, indexedChunks, failedDocIds);
    }

    private void requireEnabled() {
        if (!isEnabled()) {
            throw new IllegalStateException("AI_SEMANTIC_RETRIEVAL_ENABLED 未启用");
        }
    }

    private void validateBatch(AiEmbeddingClient.EmbeddingBatch batch, int expectedSize) {
        if (batch == null || batch.vectors() == null || batch.vectors().size() != expectedSize) {
            throw new IllegalStateException("Embedding 返回数量与知识分片数量不一致");
        }
        if (batch.dimensions() == null || batch.dimensions() <= 0) {
            throw new IllegalStateException("Embedding 向量维度无效");
        }
        for (List<Double> vector : batch.vectors()) {
            if (vector == null || vector.size() != batch.dimensions()) {
                throw new IllegalStateException("Embedding 向量维度不一致");
            }
        }
    }

    private String toJson(List<Double> vector) {
        try {
            return objectMapper.writeValueAsString(vector);
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("Embedding 向量序列化失败", failure);
        }
    }

    private int resolveBatchSize() {
        Integer configured = properties.getEmbeddingBatchSize();
        if (configured == null || configured <= 0) {
            return 16;
        }
        return Math.min(configured, 32);
    }

    public record IndexResult(
            String docId,
            int indexedChunks,
            String model,
            int dimensions,
            String status
    ) {
    }

    public record BulkIndexResult(
            int totalDocuments,
            int indexedDocuments,
            int indexedChunks,
            List<String> failedDocIds
    ) {
    }
}
