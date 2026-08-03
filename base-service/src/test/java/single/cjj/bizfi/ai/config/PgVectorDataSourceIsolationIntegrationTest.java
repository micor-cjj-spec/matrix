package single.cjj.bizfi.ai.config;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import single.cjj.bizfi.ai.entity.BizfiAiKnowledgeBaseAcl;
import single.cjj.bizfi.ai.mapper.BizfiAiKnowledgeBaseAclMapper;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(
        classes = PgVectorDataSourceIsolationIntegrationTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
class PgVectorDataSourceIsolationIntegrationTest {

    private static final DockerImageName PGVECTOR_IMAGE = DockerImageName
            .parse("pgvector/pgvector:pg16")
            .asCompatibleSubstituteFor("postgres");

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36")
            .withDatabaseName("matrix_base")
            .withUsername("matrix")
            .withPassword("matrix_test");

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(PGVECTOR_IMAGE)
            .withDatabaseName("matrix_ai")
            .withUsername("matrix_ai")
            .withPassword("matrix_ai_test");

    @DynamicPropertySource
    static void registerDataSources(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> 2);

        registry.add("bizfi.ai.vector-store.type", () -> "pgvector");
        registry.add("bizfi.ai.vector-store.dual-write-enabled", () -> true);
        registry.add("bizfi.ai.vector-store.read-fallback-enabled", () -> true);
        registry.add("bizfi.ai.vector-store.pgvector.enabled", () -> true);
        registry.add("bizfi.ai.vector-store.pgvector.jdbc-url", POSTGRES::getJdbcUrl);
        registry.add("bizfi.ai.vector-store.pgvector.username", POSTGRES::getUsername);
        registry.add("bizfi.ai.vector-store.pgvector.password", POSTGRES::getPassword);
        registry.add("bizfi.ai.vector-store.pgvector.dimensions", () -> 3);
        registry.add("bizfi.ai.vector-store.pgvector.maximum-pool-size", () -> 2);
    }

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DataSource primaryDataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    @Qualifier("transactionManager")
    private PlatformTransactionManager transactionManager;

    @Autowired
    @Qualifier("pgVectorJdbcTemplate")
    private JdbcTemplate pgVectorJdbcTemplate;

    @Autowired
    @Qualifier("pgVectorTransactionManager")
    private PlatformTransactionManager pgVectorTransactionManager;

    @Autowired
    private BizfiAiKnowledgeBaseAclMapper aclMapper;

    @Test
    void shouldKeepPrimaryAndVectorDataSourcesStrictlySeparated() throws SQLException {
        Map<String, DataSource> dataSources = applicationContext.getBeansOfType(DataSource.class);
        assertEquals(Set.of("dataSource"), dataSources.keySet());
        assertSame(primaryDataSource, dataSources.get("dataSource"));

        assertSame(primaryDataSource, jdbcTemplate.getDataSource());
        assertDatabaseProduct(primaryDataSource, "MySQL");

        DataSourceTransactionManager primaryManager = assertInstanceOf(
                DataSourceTransactionManager.class,
                transactionManager
        );
        assertSame(primaryDataSource, primaryManager.getDataSource());
        assertDatabaseProduct(primaryManager.getDataSource(), "MySQL");

        DataSource pgDataSource = pgVectorJdbcTemplate.getDataSource();
        assertNotNull(pgDataSource);
        assertDatabaseProduct(pgDataSource, "PostgreSQL");

        DataSourceTransactionManager vectorManager = assertInstanceOf(
                DataSourceTransactionManager.class,
                pgVectorTransactionManager
        );
        assertSame(pgDataSource, vectorManager.getDataSource());
        assertDatabaseProduct(vectorManager.getDataSource(), "PostgreSQL");

        jdbcTemplate.execute("CREATE TABLE primary_database_probe (fid BIGINT PRIMARY KEY, fvalue VARCHAR(32))");
        jdbcTemplate.update("INSERT INTO primary_database_probe (fid, fvalue) VALUES (?, ?)", 1L, "mysql");
        assertEquals("mysql", jdbcTemplate.queryForObject(
                "SELECT fvalue FROM primary_database_probe WHERE fid = 1",
                String.class
        ));

        pgVectorJdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
        pgVectorJdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS knowledge");
        pgVectorJdbcTemplate.execute("""
                CREATE TABLE knowledge.vector_database_probe (
                    fid BIGINT PRIMARY KEY,
                    fembedding VECTOR(3) NOT NULL
                )
                """);
        pgVectorJdbcTemplate.update(
                "INSERT INTO knowledge.vector_database_probe (fid, fembedding) VALUES (1, '[1,0,0]')"
        );
        assertEquals(1, pgVectorJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM knowledge.vector_database_probe",
                Integer.class
        ));
    }

    @Test
    void shouldResolveAclSubjectsWithIndexedMyBatisQuery() {
        jdbcTemplate.execute("""
                CREATE TABLE bizfi_ai_knowledge_base_acl (
                    fid BIGINT PRIMARY KEY AUTO_INCREMENT,
                    fkbid VARCHAR(64) NOT NULL,
                    fsubjecttype VARCHAR(32) NOT NULL,
                    fsubjectid VARCHAR(128) NOT NULL,
                    fpermission VARCHAR(32) NOT NULL,
                    fcreatedby BIGINT NOT NULL,
                    fcreatetime DATETIME NOT NULL,
                    fmodifytime DATETIME NOT NULL
                )
                """);
        insertAcl("finance", "USER", "1001", "VIEWER");
        insertAcl("finance", "ORGANIZATION", "88", "EDITOR");
        insertAcl("hr", "AUTHORITY", "department:9", "VIEWER");
        insertAcl("private", "USER", "2002", "OWNER");

        List<BizfiAiKnowledgeBaseAcl> allMatches = aclMapper.selectMatching(
                "1001",
                Set.of("88"),
                Set.of("department:9"),
                null
        );
        assertEquals(3, allMatches.size());
        assertEquals(
                Set.of("finance", "hr"),
                allMatches.stream().map(BizfiAiKnowledgeBaseAcl::getFkbid).collect(Collectors.toSet())
        );

        List<BizfiAiKnowledgeBaseAcl> financeMatches = aclMapper.selectMatching(
                "1001",
                Set.of("88"),
                Set.of("department:9"),
                "finance"
        );
        assertEquals(2, financeMatches.size());

        List<BizfiAiKnowledgeBaseAcl> directUserOnly = aclMapper.selectMatching(
                "1001",
                Set.of(),
                Set.of(),
                null
        );
        assertEquals(1, directUserOnly.size());
        assertEquals("USER", directUserOnly.get(0).getFsubjecttype());
    }

    private void insertAcl(String kbId, String subjectType, String subjectId, String permission) {
        jdbcTemplate.update("""
                        INSERT INTO bizfi_ai_knowledge_base_acl
                        (fkbid, fsubjecttype, fsubjectid, fpermission, fcreatedby, fcreatetime, fmodifytime)
                        VALUES (?, ?, ?, ?, 1001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                kbId,
                subjectType,
                subjectId,
                permission
        );
    }

    private void assertDatabaseProduct(DataSource dataSource, String expected) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            assertEquals(expected, connection.getMetaData().getDatabaseProductName());
        }
    }

    @SpringBootConfiguration
    @EnableConfigurationProperties(AiVectorStoreProperties.class)
    @Import(PgVectorDataSourceConfig.class)
    @MapperScan(basePackageClasses = BizfiAiKnowledgeBaseAclMapper.class)
    @ImportAutoConfiguration({
            DataSourceAutoConfiguration.class,
            JdbcTemplateAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            MybatisPlusAutoConfiguration.class
    })
    static class TestApplication {
    }
}
