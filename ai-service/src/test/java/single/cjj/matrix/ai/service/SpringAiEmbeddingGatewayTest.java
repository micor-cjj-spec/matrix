package single.cjj.matrix.ai.service;

import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import single.cjj.matrix.ai.api.ModelContracts;
import single.cjj.matrix.ai.config.MatrixAiProperties;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpringAiEmbeddingGatewayTest {

    @Test
    void shouldReturnBatchVectorsWithStableDimensions() {
        EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
        MatrixAiProperties properties = new MatrixAiProperties();
        properties.setEmbeddingModelName("text-embedding-test");
        SpringAiEmbeddingGateway gateway = new SpringAiEmbeddingGateway(embeddingModel, properties);

        List<String> texts = List.of("月结规则", "凭证过账");
        when(embeddingModel.embed(texts)).thenReturn(List.of(
                new float[]{1.0f, 0.0f, 0.5f},
                new float[]{0.25f, 0.75f, 0.5f}
        ));

        ModelContracts.EmbeddingResponse response = gateway.embed(
                new ModelContracts.EmbeddingRequest(texts)
        );

        assertEquals("text-embedding-test", response.model());
        assertEquals(3, response.dimensions());
        assertEquals(2, response.vectors().size());
        assertEquals(List.of(1.0, 0.0, 0.5), response.vectors().get(0));
    }

    @Test
    void shouldRejectInconsistentVectorDimensions() {
        EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
        SpringAiEmbeddingGateway gateway = new SpringAiEmbeddingGateway(
                embeddingModel,
                new MatrixAiProperties()
        );

        List<String> texts = List.of("A", "B");
        when(embeddingModel.embed(texts)).thenReturn(List.of(
                new float[]{1.0f, 0.0f},
                new float[]{1.0f}
        ));

        assertThrows(IllegalStateException.class, () -> gateway.embed(
                new ModelContracts.EmbeddingRequest(texts)
        ));
    }
}
