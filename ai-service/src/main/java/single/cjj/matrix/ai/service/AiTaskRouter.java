package single.cjj.matrix.ai.service;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import single.cjj.matrix.ai.api.ModelContracts;
import single.cjj.matrix.ai.config.MatrixAiProperties;

import java.util.Locale;

@Component
public class AiTaskRouter {

    public static final String GENERAL = "general";
    public static final String KNOWLEDGE_QA = "knowledge-qa";
    public static final String FINANCIAL_ANALYSIS = "financial-analysis";
    public static final String TOOL_CALLING = "tool-calling";
    public static final String EVALUATION = "evaluation";

    private final MatrixAiProperties properties;

    public AiTaskRouter(MatrixAiProperties properties) {
        this.properties = properties;
    }

    public ModelRoute route(ModelContracts.ChatRequest request) {
        String taskType = normalize(request == null ? null : request.taskType());
        String model = switch (taskType) {
            case KNOWLEDGE_QA -> firstConfigured(properties.getKnowledgeQaModel(), properties.getModelName());
            case FINANCIAL_ANALYSIS -> firstConfigured(
                    properties.getFinancialAnalysisModel(),
                    properties.getModelName()
            );
            case TOOL_CALLING -> firstConfigured(properties.getToolCallingModel(), properties.getModelName());
            case EVALUATION -> firstConfigured(properties.getEvaluationModel(), properties.getModelName());
            default -> properties.getModelName();
        };
        return new ModelRoute(taskType, model);
    }

    String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return GENERAL;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        return switch (normalized) {
            case "knowledge", "knowledge-qa", "rag", "qa" -> KNOWLEDGE_QA;
            case "finance", "financial", "financial-analysis", "analysis" -> FINANCIAL_ANALYSIS;
            case "tool", "tools", "tool-calling", "agent" -> TOOL_CALLING;
            case "eval", "evaluation", "judge" -> EVALUATION;
            case "general", "chat", "default" -> GENERAL;
            default -> GENERAL;
        };
    }

    private String firstConfigured(String preferred, String fallback) {
        return StringUtils.hasText(preferred) ? preferred.trim() : fallback;
    }

    public record ModelRoute(String taskType, String model) {
    }
}
