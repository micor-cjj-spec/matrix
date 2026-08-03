package single.cjj.bizfi.ai.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

    @Bean(name = "pgVectorDataSource", destroyMethod = "close")
    public HikariDataSource pgVectorDataSource(AiVectorStoreProperties properties) {
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
        return new HikariDataSource(config);
    }

    @Bean(name = "pgVectorJdbcTemplate")
    public JdbcTemplate pgVectorJdbcTemplate(
            @Qualifier("pgVectorDataSource") DataSource dataSource
    ) {
        return new JdbcTemplate(dataSource);
    }

    @Bean(name = "pgVectorTransactionManager")
    public PlatformTransactionManager pgVectorTransactionManager(
            @Qualifier("pgVectorDataSource") DataSource dataSource
    ) {
        return new DataSourceTransactionManager(dataSource);
    }

    private int resolvePositive(Integer value, int fallback) {
        return value != null && value > 0 ? value : fallback;
    }

    private long resolvePositive(Long value, long fallback) {
        return value != null && value > 0L ? value : fallback;
    }
}
