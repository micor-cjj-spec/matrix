package single.cjj.matrix.ai.service;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import single.cjj.matrix.ai.api.ModelContracts;
import single.cjj.matrix.ai.config.MatrixAiProperties;

import java.util.ArrayList;
import java.util.List;

@Component
public class SpringAiEmbeddingGateway {

    private final EmbeddingModel embeddingModel;
    private final MatrixAiProperties properties;

    public SpringAiEmbeddingGateway(
            EmbeddingModel embeddingModel,
            MatrixAiProperties properties
    ) {
        this.embeddingModel = embeddingModel;
        this.properties = properties;
    }

    public ModelContracts.EmbeddingResponse embed(ModelContracts.EmbeddingRequest request) {
        if (request == null || request.texts() == null || request.texts().isEmpty()) {
            throw new IllegalArgumentException("embedding texts 不能为空");
        }

        List<String> texts = request.texts().stream()
                .map(text -> text == null ? "" : text.trim())
                .filter(StringUtils::hasText)
                .toList();
        if (texts.size() != request.texts().size()) {
            throw new IllegalArgumentException("embedding texts 不能包含空文本");
        }

        List<float[]> rawVectors = embeddingModel.embed(texts);
        if (rawVectors == null || rawVectors.size() != texts.size()) {
            throw new IllegalStateException("Embedding 模型返回数量与请求不一致");
        }

        Integer dimensions = null;
        List<List<Double>> vectors = new ArrayList<>(rawVectors.size());
        for (float[] rawVector : rawVectors) {
            if (rawVector == null || rawVector.length == 0) {
                throw new IllegalStateException("Embedding 模型返回空向量");
            }
            if (dimensions == null) {
                dimensions = rawVector.length;
            } else if (dimensions != rawVector.length) {
                throw new IllegalStateException("Embedding 模型返回的向量维度不一致");
            }

            List<Double> vector = new ArrayList<>(rawVector.length);
            for (float value : rawVector) {
                if (!Float.isFinite(value)) {
                    throw new IllegalStateException("Embedding 模型返回非法数值");
                }
                vector.add((double) value);
            }
            vectors.add(vector);
        }

        return new ModelContracts.EmbeddingResponse(
                properties.getEmbeddingModelName(),
                dimensions == null ? 0 : dimensions,
                vectors
        );
    }
}
