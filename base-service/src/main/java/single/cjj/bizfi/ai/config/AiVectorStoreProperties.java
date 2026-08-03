package single.cjj.bizfi.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "bizfi.ai.vector-store")
public class AiVectorStoreProperties {

    public static final String MYSQL_JSON = "mysql-json";
    public static final String PGVECTOR = "pgvector";

    /**
     * Semantic read backend. Supported values: mysql-json, pgvector.
     */
    private String type = MYSQL_JSON;

    /**
     * Write generated embeddings to both MySQL JSON and PGVector during rollout.
     */
    private Boolean dualWriteEnabled = false;

    /**
     * Fall back to the legacy MySQL JSON semantic retriever when PGVector is unavailable.
     */
    private Boolean readFallbackEnabled = true;

    private PgVector pgvector = new PgVector();

    public boolean usePgVectorForRead() {
        return PGVECTOR.equalsIgnoreCase(normalize(type));
    }

    public boolean shouldWritePgVector() {
        return Boolean.TRUE.equals(pgvector.getEnabled())
                && (usePgVectorForRead() || Boolean.TRUE.equals(dualWriteEnabled));
    }

    public boolean shouldWriteMysqlJson() {
        return !usePgVectorForRead() || Boolean.TRUE.equals(dualWriteEnabled);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    @Data
    public static class PgVector {
        private Boolean enabled = false;
        private String jdbcUrl = "jdbc:postgresql://127.0.0.1:5433/matrix_ai";
        private String username = "matrix_ai";
        private String password = "";
        private String schema = "knowledge";
        private String table = "matrix_ai_knowledge_vector";
        private Integer dimensions = 1536;
        private Integer maximumPoolSize = 5;
        private Long connectionTimeoutMs = 5000L;
    }
}
