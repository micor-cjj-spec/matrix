package single.cjj.bizfi.ai.config;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers(disabledWithoutDocker = true)
class RagEvaluationSchemaIntegrationTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36")
            .withDatabaseName("matrix_base")
            .withUsername("matrix")
            .withPassword("matrix_test");

    @Test
    void shouldApplyEvaluationMigrationIdempotently() throws IOException {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(new DriverManagerDataSource(
                MYSQL.getJdbcUrl(),
                MYSQL.getUsername(),
                MYSQL.getPassword()
        ));
        String migration = Files.readString(resolveMigrationPath());

        applyMigration(jdbcTemplate, migration);
        applyMigration(jdbcTemplate, migration);

        assertTableExists(jdbcTemplate, "bizfi_ai_rag_eval_set");
        assertTableExists(jdbcTemplate, "bizfi_ai_rag_eval_case");
        assertTableExists(jdbcTemplate, "bizfi_ai_rag_eval_run");
        assertTableExists(jdbcTemplate, "bizfi_ai_rag_eval_result");
    }

    private void applyMigration(JdbcTemplate jdbcTemplate, String migration) {
        String withoutComments = migration.replaceAll("(?m)^\\s*--.*$", "");
        for (String statement : withoutComments.split(";")) {
            if (!statement.isBlank()) {
                jdbcTemplate.execute(statement.trim());
            }
        }
    }

    private void assertTableExists(JdbcTemplate jdbcTemplate, String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables "
                        + "WHERE table_schema = DATABASE() AND table_name = ?",
                Integer.class,
                tableName
        );
        assertEquals(1, count);
    }

    private Path resolveMigrationPath() {
        return List.of(
                        Path.of("sql", "bizfi_ai_rag_evaluation_v7.sql"),
                        Path.of("..", "sql", "bizfi_ai_rag_evaluation_v7.sql")
                ).stream()
                .filter(Files::isRegularFile)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("V7 evaluation migration file not found"));
    }
}
