package single.cjj.bizfi.ai.service.impl;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import single.cjj.bizfi.ai.config.AiVectorStoreProperties;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Service
public class AiVectorStoreDiagnosticsService {

    private final DataSource primaryDataSource;
    private final AiVectorStoreProperties properties;
    private final ObjectProvider<PgVectorKnowledgeRepository> pgVectorRepositoryProvider;

    public AiVectorStoreDiagnosticsService(
            DataSource primaryDataSource,
            AiVectorStoreProperties properties,
            ObjectProvider<PgVectorKnowledgeRepository> pgVectorRepositoryProvider
    ) {
        this.primaryDataSource = primaryDataSource;
        this.properties = properties;
        this.pgVectorRepositoryProvider = pgVectorRepositoryProvider;
    }

    public VectorStoreStatus status() {
        boolean pgVectorEnabled = Boolean.TRUE.equals(properties.getPgvector().getEnabled());
        PgVectorKnowledgeRepository repository = pgVectorRepositoryProvider.getIfAvailable();
        boolean pgVectorReady = repository != null && repository.isReady();
        String vectorDatabase = !pgVectorEnabled
                ? "DISABLED"
                : repository == null ? "UNAVAILABLE" : repository.databaseProductName();

        return new VectorStoreStatus(
                properties.getType(),
                Boolean.TRUE.equals(properties.getDualWriteEnabled()),
                Boolean.TRUE.equals(properties.getReadFallbackEnabled()),
                pgVectorEnabled,
                pgVectorReady,
                properties.getPgvector().getDimensions(),
                databaseProductName(primaryDataSource),
                vectorDatabase
        );
    }

    private String databaseProductName(DataSource dataSource) {
        if (dataSource == null) {
            return "UNAVAILABLE";
        }
        try (Connection connection = dataSource.getConnection()) {
            return connection.getMetaData().getDatabaseProductName();
        } catch (SQLException | RuntimeException unavailable) {
            return "UNAVAILABLE";
        }
    }

    public record VectorStoreStatus(
            String readStore,
            boolean dualWriteEnabled,
            boolean readFallbackEnabled,
            boolean pgVectorEnabled,
            boolean pgVectorReady,
            Integer dimensions,
            String primaryDatabase,
            String vectorDatabase
    ) {
    }
}
