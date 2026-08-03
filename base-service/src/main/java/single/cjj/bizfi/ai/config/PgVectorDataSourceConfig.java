package single.cjj.bizfi.ai.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
@ConditionalOnProperty(
        prefix = "bizfi.ai.vector-store.pgvector",
        name = "enabled",
        havingValue = "true"
)
public class PgVectorDataSourceConfig {

    private HikariDataSource pgVectorDataSource;

    @Bean(name = "jdbcTemplate")
    @Primary
    @ConditionalOnMissingBean(name = "jdbcTemplate")
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean(name = "pgVectorJdbcTemplate")
    public JdbcTemplate pgVectorJdbcTemplate(AiVectorStoreProperties properties) {
        return new JdbcTemplate(pgVectorDataSource(properties));
    }

    @Bean(name = "pgVectorTransactionManager")
    public PlatformTransactionManager pgVectorTransactionManager(AiVectorStoreProperties properties) {
        return new DataSourceTransactionManager(pgVectorDataSource(properties));
    }

    @PreDestroy
    public void closePgVectorDataSource() {
        if (pgVectorDataSource != null) {
            pgVectorDataSource.close();
        }
    }

    private synchronized HikariDataSource pgVectorDataSource(AiVectorStoreProperties properties) {
        if (pgVectorDataSource != null) {
            return pgVectorDataSource;
        }
        AiVectorStoreProperties.PgVector settings = properties.getPgvector();
        HikariConfig config = new HikariConfig();
        config.setPoolName("matrix-pgvector-pool");
        config.setJdbcUrl(settings.getJdbcUrl());
        config.setUsername(settings.getUsername());
        config.setPassword(settings.getPassword());
        config.setDriverClassName("org.postgresql.Driver");
        config.setMaximumPoolSize(resolvePositive(settings.getMaximumPoolSize(), 5));
        config.setMinimumIdle(0);
        config.setConnectionTimeout(resolvePositive(settings.getConnectionTimeoutMs(), 5000L));
        config.setValidationTimeout(Math.min(3000L, config.getConnectionTimeout()));
        pgVectorDataSource = new HikariDataSource(config);
        return pgVectorDataSource;
    }

    private int resolvePositive(Integer value, int fallback) {
        return value != null && value > 0 ? value : fallback;
    }

    private long resolvePositive(Long value, long fallback) {
        return value != null && value > 0L ? value : fallback;
    }
}
