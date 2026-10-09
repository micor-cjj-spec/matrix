package single.cjj.bizfi.ai.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import single.cjj.bizfi.ai.config.AiVectorStoreProperties;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiVectorStoreDiagnosticsServiceTest {

    @Mock
    private DataSource primaryDataSource;

    @Mock
    private Connection primaryConnection;

    @Mock
    private DatabaseMetaData primaryMetadata;

    @Mock
    private ObjectProvider<PgVectorKnowledgeRepository> repositoryProvider;

    @Mock
    private PgVectorKnowledgeRepository repository;

    private AiVectorStoreProperties properties;

    @BeforeEach
    void setUp() {
        properties = new AiVectorStoreProperties();
    }

    @Test
    void shouldReportActualPrimaryAndVectorDatabaseProducts() throws Exception {
        properties.setType(AiVectorStoreProperties.PGVECTOR);
        properties.setDualWriteEnabled(true);
        properties.getPgvector().setEnabled(true);
        properties.getPgvector().setDimensions(1536);

        when(primaryDataSource.getConnection()).thenReturn(primaryConnection);
        when(primaryConnection.getMetaData()).thenReturn(primaryMetadata);
        when(primaryMetadata.getDatabaseProductName()).thenReturn("MySQL");
        when(repositoryProvider.getIfAvailable()).thenReturn(repository);
        when(repository.isReady()).thenReturn(true);
        when(repository.databaseProductName()).thenReturn("PostgreSQL");

        AiVectorStoreDiagnosticsService service = new AiVectorStoreDiagnosticsService(
                primaryDataSource,
                properties,
                repositoryProvider
        );

        AiVectorStoreDiagnosticsService.VectorStoreStatus status = service.status();

        assertEquals("pgvector", status.readStore());
        assertTrue(status.dualWriteEnabled());
        assertTrue(status.pgVectorEnabled());
        assertTrue(status.pgVectorReady());
        assertEquals(1536, status.dimensions());
        assertEquals("MySQL", status.primaryDatabase());
        assertEquals("PostgreSQL", status.vectorDatabase());
    }

    @Test
    void shouldReportDisabledVectorDatabaseWithoutRepository() throws Exception {
        when(primaryDataSource.getConnection()).thenReturn(primaryConnection);
        when(primaryConnection.getMetaData()).thenReturn(primaryMetadata);
        when(primaryMetadata.getDatabaseProductName()).thenReturn("MySQL");

        AiVectorStoreDiagnosticsService service = new AiVectorStoreDiagnosticsService(
                primaryDataSource,
                properties,
                repositoryProvider
        );

        AiVectorStoreDiagnosticsService.VectorStoreStatus status = service.status();

        assertEquals("mysql-json", status.readStore());
        assertEquals("MySQL", status.primaryDatabase());
        assertEquals("DISABLED", status.vectorDatabase());
    }
}
