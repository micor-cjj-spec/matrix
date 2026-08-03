package single.cjj.bizfi.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.ai.config.AiProperties;
import single.cjj.bizfi.ai.config.AiVectorStoreProperties;
import single.cjj.bizfi.ai.entity.BizfiAiKnowledgeChunk;
import single.cjj.bizfi.ai.mapper.BizfiAiKnowledgeChunkMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AiKnowledgeVectorIndexService {

    private static final Logger log = LoggerFactory.getLogger(AiKnowledgeVectorIndexService.class);

    private final BizfiAiKnowledgeChunkMapper chunkMapper;
    private final AiEmbeddingClient embeddingClient;
    private final ObjectMapper objectMapper;
    private final AiProperties properties;
    private final AiVectorStoreProperties vectorStoreProperties;
    private final ObjectProvider<PgVectorKnowledgeRepository> pgVectorRepositoryProvider;

    public AiKnowledgeVectorIndexService(
            BizfiAiKnowledgeChunkMapper chunkMapper,
            AiEmbeddingClient embeddingClient,
            ObjectMapper objectMapper,
            AiProperties properties,
            AiVectorStoreProperties vectorStoreProperties,
            ObjectProvider<PgVectorKnowledgeRepository> pgVectorRepositoryProvider
    ) {
        this.chunkMapper = chunkMapper;
        this.embeddingClient = embeddingClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.vectorStoreProperties = vectorStoreProperties;
        this.pgVectorRepositoryProvider = pgVectorRepositoryProvider;
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
        String normalizedDocId = docId.trim();

        List<BizfiAiKnowledgeChunk> chunks = chunkMapper.selectList(
                new LambdaQueryWrapper<BizfiAiKnowledgeChunk>()
                        .eq(BizfiAiKnowledgeChunk::getFdocid, normalizedDocId)
                        .orderByAsc(BizfiAiKnowledgeChunk::getFseq)
        );
        if (chunks == null || chunks.isEmpty()) {
            deleteDocument(normalizedDocId);
            return new IndexResult(normalizedDocId, 0, null, 0, "EMPTY");
        }

        int batchSize = resolveBatchSize();
        int indexed = 0;
        String model = null;
        Integer dimensions = null;
        List<PgVectorKnowledgeRepository.KnowledgeVectorRecord> pgVectorRecords = new ArrayList<>();

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
                List<Double> vector = batch.vectors().get(offset);
                persistMysqlIndexMetadata(chunk, vector, model, dimensions, indexedAt);
                pgVectorRecords.add(toPgVectorRecord(chunk, vector, model, dimensions, indexedAt));
                indexed++;
            }
        }

        boolean pgVectorWriteSucceeded = writePgVectorIfRequired(normalizedDocId, pgVectorRecords);
        String status = vectorStoreProperties.shouldWritePgVector() && !pgVectorWriteSucceeded
                ? "PARTIAL"
                : "INDEXED";
        return new IndexResult(
                normalizedDocId,
                indexed,
                model,
                dimensions == null ? 0 : dimensions,
                status
        );
    }

    public BulkIndexResult reindexAll(boolean onlyMissing) {
        requireEnabled();
        LambdaQueryWrapper<BizfiAiKnowledgeChunk> wrapper = new LambdaQueryWrapper<>();
        if (onlyMissing && vectorStoreProperties.shouldWriteMysqlJson()) {
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
        for (String currentDocId : docIds) {
            try {
                IndexResult result = reindexDocument(currentDocId);
                if (!"FAILED".equals(result.status())) {
                    indexedDocuments++;
                    indexedChunks += result.indexedChunks();
                }
                if ("PARTIAL".equals(result.status())) {
                    failedDocIds.add(currentDocId);
                }
            } catch (RuntimeException failure) {
                failedDocIds.add(currentDocId);
                if (!Boolean.TRUE.equals(properties.getSemanticFailOpen())) {
                    throw failure;
                }
                log.warn("Bulk knowledge embedding indexing failed. docId={}", currentDocId, failure);
            }
        }
        return new BulkIndexResult(docIds.size(), indexedDocuments, indexedChunks, failedDocIds);
    }

    public BulkIndexResult migrateAllToPgVector() {
        requireEnabled();
        if (!Boolean.TRUE.equals(vectorStoreProperties.getPgvector().getEnabled())) {
            throw new IllegalStateException("AI_PGVECTOR_ENABLED 未启用");
        }
        if (!vectorStoreProperties.shouldWritePgVector()) {
            throw new IllegalStateException(
                    "迁移前请启用 AI_VECTOR_DUAL_WRITE_ENABLED，或将 AI_VECTOR_STORE_TYPE 设置为 pgvector"
            );
        }
        return reindexAll(false);
    }

    public void deleteDocument(String docId) {
        if (!StringUtils.hasText(docId) || !Boolean.TRUE.equals(vectorStoreProperties.getPgvector().getEnabled())) {
            return;
        }
        PgVectorKnowledgeRepository repository = pgVectorRepositoryProvider.getIfAvailable();
        if (repository == null) {
            if (vectorStoreProperties.shouldWritePgVector()
                    && !Boolean.TRUE.equals(properties.getSemanticFailOpen())) {
                throw new IllegalStateException("PGVector Repository 未启用");
            }
            return;
        }
        try {
            repository.deleteByDocumentId(docId.trim());
        } catch (RuntimeException failure) {
            if (!Boolean.TRUE.equals(properties.getSemanticFailOpen())) {
                throw failure;
            }
            log.warn("Delete PGVector knowledge records failed. docId={}", docId, failure);
        }
    }

    public VectorStoreStatus vectorStoreStatus() {
        PgVectorKnowledgeRepository repository = pgVectorRepositoryProvider.getIfAvailable();
        boolean pgVectorReady = repository != null && repository.isReady();
        return new VectorStoreStatus(
                vectorStoreProperties.getType(),
                Boolean.TRUE.equals(vectorStoreProperties.getDualWriteEnabled()),
                Boolean.TRUE.equals(vectorStoreProperties.getReadFallbackEnabled()),
                Boolean.TRUE.equals(vectorStoreProperties.getPgvector().getEnabled()),
                pgVectorReady,
                vectorStoreProperties.getPgvector().getDimensions()
        );
    }

    private void persistMysqlIndexMetadata(
            BizfiAiKnowledgeChunk chunk,
            List<Double> vector,
            String model,
            Integer dimensions,
            LocalDateTime indexedAt
    ) {
        if (vectorStoreProperties.shouldWriteMysqlJson()) {
            chunk.setFembedding(toJson(vector));
        }
        chunk.setFembeddingmodel(model);
        chunk.setFembeddingdimensions(dimensions);
        chunk.setFembeddingtime(indexedAt);
        chunkMapper.updateById(chunk);
    }

    private PgVectorKnowledgeRepository.KnowledgeVectorRecord toPgVectorRecord(
            BizfiAiKnowledgeChunk chunk,
            List<Double> vector,
            String model,
            int dimensions,
            LocalDateTime indexedAt
    ) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (chunk.getFseq() != null) {
            metadata.put("sequence", chunk.getFseq());
        }
        metadata.put("source", "matrix-knowledge");
        return new PgVectorKnowledgeRepository.KnowledgeVectorRecord(
                chunk.getFchunkid(),
                chunk.getFdocid(),
                chunk.getFcontent(),
                sha256(chunk.getFcontent()),
                toJson(metadata),
                vector,
                model,
                dimensions,
                indexedAt
        );
    }

    private boolean writePgVectorIfRequired(
            String docId,
            List<PgVectorKnowledgeRepository.KnowledgeVectorRecord> records
    ) {
        if (!vectorStoreProperties.shouldWritePgVector()) {
            return true;
        }
        PgVectorKnowledgeRepository repository = pgVectorRepositoryProvider.getIfAvailable();
        if (repository == null) {
            return handlePgVectorWriteFailure(
                    docId,
                    new IllegalStateException("PGVector Repository 未启用")
            );
        }
        try {
            repository.replaceDocument(docId, records);
            return true;
        } catch (RuntimeException failure) {
            return handlePgVectorWriteFailure(docId, failure);
        }
    }

    private boolean handlePgVectorWriteFailure(String docId, RuntimeException failure) {
        if (!Boolean.TRUE.equals(properties.getSemanticFailOpen())) {
            throw failure;
        }
        log.warn("Write PGVector knowledge records failed. docId={}", docId, failure);
        return false;
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
        if (vectorStoreProperties.shouldWritePgVector()) {
            Integer configuredDimensions = vectorStoreProperties.getPgvector().getDimensions();
            if (configuredDimensions != null
                    && configuredDimensions > 0
                    && !configuredDimensions.equals(batch.dimensions())) {
                throw new IllegalStateException(
                        "Embedding 维度 " + batch.dimensions()
                                + " 与 PGVector 配置维度 " + configuredDimensions + " 不一致"
                );
            }
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("知识向量数据序列化失败", failure);
        }
    }

    private String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((content == null ? "" : content).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException("JVM 不支持 SHA-256", failure);
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

    public record VectorStoreStatus(
            String readStore,
            boolean dualWriteEnabled,
            boolean readFallbackEnabled,
            boolean pgVectorEnabled,
            boolean pgVectorReady,
            Integer dimensions
    ) {
    }
}
