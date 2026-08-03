package single.cjj.bizfi.ai.service.impl;

import com.pgvector.PGvector;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.ai.config.AiVectorStoreProperties;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Repository
@ConditionalOnBean(name = "pgVectorJdbcTemplate")
public class PgVectorKnowledgeRepository {

    private static final Pattern SQL_IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");
    private static final int MAX_SEARCH_TOP_K = 100;

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final AiVectorStoreProperties properties;
    private final String qualifiedTable;

    public PgVectorKnowledgeRepository(
            @Qualifier("pgVectorJdbcTemplate") JdbcTemplate jdbcTemplate,
            @Qualifier("pgVectorTransactionManager") PlatformTransactionManager transactionManager,
            AiVectorStoreProperties properties
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.properties = properties;
        this.qualifiedTable = qualifiedTable(properties.getPgvector().getSchema(), properties.getPgvector().getTable());
    }

    public void replaceDocument(String documentId, List<KnowledgeVectorRecord> records) {
        if (!StringUtils.hasText(documentId)) {
            throw new IllegalArgumentException("documentId 不能为空");
        }
        List<KnowledgeVectorRecord> safeRecords = records == null ? List.of() : List.copyOf(records);
        for (KnowledgeVectorRecord record : safeRecords) {
            validateRecord(record, documentId.trim());
        }

        transactionTemplate.executeWithoutResult(status -> {
            deleteByDocumentIdInternal(documentId.trim());
            if (!safeRecords.isEmpty()) {
                batchUpsert(safeRecords);
            }
        });
    }

    public void deleteByDocumentId(String documentId) {
        if (!StringUtils.hasText(documentId)) {
            return;
        }
        jdbcTemplate.update(
                "DELETE FROM " + qualifiedTable + " WHERE fdocument_id = ?",
                documentId.trim()
        );
    }

    public List<VectorSearchResult> similaritySearch(
            List<Double> queryVector,
            String embeddingModel,
            Set<String> allowedDocumentIds,
            int topK
    ) {
        float[] vector = toFloatArray(queryVector);
        validateDimensions(vector.length);
        if (!StringUtils.hasText(embeddingModel)) {
            throw new IllegalArgumentException("embeddingModel 不能为空");
        }

        List<String> documentIds = normalizeDocumentIds(allowedDocumentIds);
        int limit = Math.max(1, Math.min(topK, MAX_SEARCH_TOP_K));
        StringBuilder sql = new StringBuilder()
                .append("SELECT fchunk_id, fdocument_id, fcontent, ")
                .append("1 - (fembedding <=> ?) AS similarity ")
                .append("FROM ").append(qualifiedTable).append(' ')
                .append("WHERE fstatus = 'ACTIVE' ")
                .append("AND fembedding_model = ? ")
                .append("AND fembedding_dimensions = ? ");
        if (!documentIds.isEmpty()) {
            sql.append("AND fdocument_id IN (")
                    .append(String.join(",", documentIds.stream().map(item -> "?").toList()))
                    .append(") ");
        }
        sql.append("ORDER BY fembedding <=> ? LIMIT ?");

        PGvector pgVector = new PGvector(vector);
        return jdbcTemplate.query(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql.toString());
            int parameter = 1;
            statement.setObject(parameter++, pgVector);
            statement.setString(parameter++, embeddingModel.trim());
            statement.setInt(parameter++, vector.length);
            for (String documentId : documentIds) {
                statement.setString(parameter++, documentId);
            }
            statement.setObject(parameter++, pgVector);
            statement.setInt(parameter, limit);
            return statement;
        }, (resultSet, rowNumber) -> new VectorSearchResult(
                resultSet.getString("fchunk_id"),
                resultSet.getString("fdocument_id"),
                resultSet.getString("fcontent"),
                resultSet.getDouble("similarity")
        ));
    }

    public int countByDocumentId(String documentId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + qualifiedTable + " WHERE fdocument_id = ?",
                Integer.class,
                documentId
        );
        return count == null ? 0 : count;
    }

    public boolean isReady() {
        try {
            Integer value = jdbcTemplate.queryForObject(
                    "SELECT 1 FROM " + qualifiedTable + " LIMIT 1",
                    Integer.class
            );
            return value != null && value == 1;
        } catch (RuntimeException ignored) {
            try {
                jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + qualifiedTable, Long.class);
                return true;
            } catch (RuntimeException unavailable) {
                return false;
            }
        }
    }

    private void deleteByDocumentIdInternal(String documentId) {
        jdbcTemplate.update(
                "DELETE FROM " + qualifiedTable + " WHERE fdocument_id = ?",
                documentId
        );
    }

    private void batchUpsert(List<KnowledgeVectorRecord> records) {
        String sql = "INSERT INTO " + qualifiedTable + " ("
                + "fchunk_id, fdocument_id, fcontent, fcontent_hash, fmetadata, "
                + "fembedding, fembedding_model, fembedding_dimensions, fstatus, fcreate_time, fmodify_time"
                + ") VALUES (?, ?, ?, ?, CAST(? AS jsonb), ?, ?, ?, 'ACTIVE', ?, ?) "
                + "ON CONFLICT (fchunk_id, fembedding_model) DO UPDATE SET "
                + "fdocument_id = EXCLUDED.fdocument_id, "
                + "fcontent = EXCLUDED.fcontent, "
                + "fcontent_hash = EXCLUDED.fcontent_hash, "
                + "fmetadata = EXCLUDED.fmetadata, "
                + "fembedding = EXCLUDED.fembedding, "
                + "fembedding_dimensions = EXCLUDED.fembedding_dimensions, "
                + "fstatus = 'ACTIVE', "
                + "fmodify_time = EXCLUDED.fmodify_time";

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement statement, int index) throws SQLException {
                KnowledgeVectorRecord record = records.get(index);
                LocalDateTime now = record.indexedAt() == null ? LocalDateTime.now() : record.indexedAt();
                statement.setString(1, record.chunkId());
                statement.setString(2, record.documentId());
                statement.setString(3, record.content());
                statement.setString(4, record.contentHash());
                statement.setString(5, StringUtils.hasText(record.metadataJson()) ? record.metadataJson() : "{}");
                statement.setObject(6, new PGvector(toFloatArray(record.embedding())));
                statement.setString(7, record.embeddingModel());
                statement.setInt(8, record.dimensions());
                statement.setObject(9, now);
                statement.setObject(10, now);
            }

            @Override
            public int getBatchSize() {
                return records.size();
            }
        });
    }

    private void validateRecord(KnowledgeVectorRecord record, String expectedDocumentId) {
        if (record == null) {
            throw new IllegalArgumentException("knowledge vector record 不能为空");
        }
        if (!expectedDocumentId.equals(record.documentId())) {
            throw new IllegalArgumentException("同一批次只能替换一个文档的向量");
        }
        if (!StringUtils.hasText(record.chunkId()) || !StringUtils.hasText(record.content())) {
            throw new IllegalArgumentException("chunkId 和 content 不能为空");
        }
        if (!StringUtils.hasText(record.embeddingModel()) || !StringUtils.hasText(record.contentHash())) {
            throw new IllegalArgumentException("embeddingModel 和 contentHash 不能为空");
        }
        float[] embedding = toFloatArray(record.embedding());
        if (record.dimensions() != embedding.length) {
            throw new IllegalArgumentException("记录维度与向量长度不一致");
        }
        validateDimensions(embedding.length);
    }

    private void validateDimensions(int dimensions) {
        Integer configured = properties.getPgvector().getDimensions();
        int expected = configured == null || configured <= 0 ? 1536 : configured;
        if (dimensions != expected) {
            throw new IllegalArgumentException(
                    "Embedding 维度 " + dimensions + " 与 PGVector 配置维度 " + expected + " 不一致"
            );
        }
    }

    private float[] toFloatArray(List<Double> values) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("Embedding 向量不能为空");
        }
        float[] result = new float[values.size()];
        for (int index = 0; index < values.size(); index++) {
            Double value = values.get(index);
            if (value == null || !Double.isFinite(value)) {
                throw new IllegalArgumentException("Embedding 向量包含非法数值");
            }
            result[index] = value.floatValue();
        }
        return result;
    }

    private List<String> normalizeDocumentIds(Set<String> allowedDocumentIds) {
        if (allowedDocumentIds == null || allowedDocumentIds.isEmpty()) {
            return List.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String documentId : allowedDocumentIds) {
            if (StringUtils.hasText(documentId)) {
                normalized.add(documentId.trim());
            }
        }
        return new ArrayList<>(normalized);
    }

    private String qualifiedTable(String schema, String table) {
        return validateIdentifier(schema, "schema") + "." + validateIdentifier(table, "table");
    }

    private String validateIdentifier(String value, String field) {
        if (!StringUtils.hasText(value) || !SQL_IDENTIFIER.matcher(value.trim()).matches()) {
            throw new IllegalArgumentException("非法 PGVector " + field + " 标识符");
        }
        return value.trim();
    }

    public record KnowledgeVectorRecord(
            String chunkId,
            String documentId,
            String content,
            String contentHash,
            String metadataJson,
            List<Double> embedding,
            String embeddingModel,
            int dimensions,
            LocalDateTime indexedAt
    ) {
    }

    public record VectorSearchResult(
            String chunkId,
            String documentId,
            String content,
            double similarity
    ) {
    }
}
