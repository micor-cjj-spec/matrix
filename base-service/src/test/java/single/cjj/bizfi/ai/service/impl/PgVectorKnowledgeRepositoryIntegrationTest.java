package single.cjj.bizfi.ai.service.impl;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import single.cjj.bizfi.ai.config.AiVectorStoreProperties;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers(disabledWithoutDocker = true)
class PgVectorKnowledgeRepositoryIntegrationTest {

    private static final DockerImageName PGVECTOR_IMAGE = DockerImageName
            .parse("pgvector/pgvector:pg16")
            .asCompatibleSubstituteFor("postgres");

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(PGVECTOR_IMAGE)
            .withDatabaseName("matrix_ai")
            .withUsername("matrix_ai")
            .withPassword("matrix_ai_test");

    private static HikariDataSource dataSource;
    private static PgVectorKnowledgeRepository repository;

    @BeforeAll
    static void setUpDatabase() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(POSTGRES.getJdbcUrl());
        hikariConfig.setUsername(POSTGRES.getUsername());
        hikariConfig.setPassword(POSTGRES.getPassword());
        hikariConfig.setMaximumPoolSize(2);
        dataSource = new HikariDataSource(hikariConfig);

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS knowledge");
        jdbcTemplate.execute("""
                CREATE TABLE knowledge.matrix_ai_knowledge_vector (
                    fid BIGSERIAL PRIMARY KEY,
                    fchunk_id VARCHAR(160) NOT NULL,
                    fdocument_id VARCHAR(128) NOT NULL,
                    fcontent TEXT NOT NULL,
                    fcontent_hash VARCHAR(64) NOT NULL,
                    fmetadata JSONB NOT NULL DEFAULT '{}'::jsonb,
                    fembedding VECTOR(3) NOT NULL,
                    fembedding_model VARCHAR(128) NOT NULL,
                    fembedding_dimensions INT NOT NULL,
                    fstatus VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
                    fcreate_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    fmodify_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE (fchunk_id, fembedding_model)
                )
                """);
        jdbcTemplate.execute("""
                CREATE INDEX idx_test_vector_hnsw
                ON knowledge.matrix_ai_knowledge_vector
                USING hnsw (fembedding vector_cosine_ops)
                """);

        AiVectorStoreProperties properties = new AiVectorStoreProperties();
        properties.getPgvector().setEnabled(true);
        properties.getPgvector().setDimensions(3);
        repository = new PgVectorKnowledgeRepository(
                jdbcTemplate,
                new DataSourceTransactionManager(dataSource),
                properties
        );
    }

    @AfterAll
    static void closeDataSource() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @Test
    void shouldReplaceSearchAndDeleteDocumentVectors() {
        repository.replaceDocument("doc_1", List.of(
                record("chunk_1", "第一段", List.of(1.0D, 0.0D, 0.0D)),
                record("chunk_2", "第二段", List.of(0.0D, 1.0D, 0.0D))
        ));

        List<PgVectorKnowledgeRepository.VectorSearchResult> results = repository.similaritySearch(
                List.of(0.9D, 0.1D, 0.0D),
                "embedding-test",
                Set.of("doc_1"),
                2
        );

        assertEquals(2, results.size());
        assertEquals("chunk_1", results.get(0).chunkId());
        assertEquals(2, repository.countByDocumentId("doc_1"));

        repository.replaceDocument("doc_1", List.of(
                record("chunk_3", "替换后的分片", List.of(0.0D, 0.0D, 1.0D))
        ));
        assertEquals(1, repository.countByDocumentId("doc_1"));

        repository.deleteByDocumentId("doc_1");
        assertEquals(0, repository.countByDocumentId("doc_1"));
    }

    private static PgVectorKnowledgeRepository.KnowledgeVectorRecord record(
            String chunkId,
            String content,
            List<Double> embedding
    ) {
        return new PgVectorKnowledgeRepository.KnowledgeVectorRecord(
                chunkId,
                "doc_1",
                content,
                "hash-" + chunkId,
                "{\"sequence\":1}",
                embedding,
                "embedding-test",
                3,
                LocalDateTime.now()
        );
    }
}
