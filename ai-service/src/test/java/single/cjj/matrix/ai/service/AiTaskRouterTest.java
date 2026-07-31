package single.cjj.matrix.ai.service;

import org.junit.jupiter.api.Test;
import single.cjj.matrix.ai.api.ModelContracts;
import single.cjj.matrix.ai.config.MatrixAiProperties;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiTaskRouterTest {

    private final MatrixAiProperties properties = properties();
    private final AiTaskRouter router = new AiTaskRouter(properties);

    @Test
    void shouldUseDefaultModelForMissingOrUnknownTask() {
        assertRoute(null, AiTaskRouter.GENERAL, "default-model");
        assertRoute("unexpected", AiTaskRouter.GENERAL, "default-model");
    }

    @Test
    void shouldNormalizeAliasesAndUseConfiguredModels() {
        assertRoute("rag", AiTaskRouter.KNOWLEDGE_QA, "knowledge-model");
        assertRoute("financial_analysis", AiTaskRouter.FINANCIAL_ANALYSIS, "analysis-model");
        assertRoute("agent", AiTaskRouter.TOOL_CALLING, "tool-model");
        assertRoute("judge", AiTaskRouter.EVALUATION, "evaluation-model");
    }

    @Test
    void shouldFallBackToDefaultWhenTaskModelIsBlank() {
        properties.setToolCallingModel(" ");

        assertRoute("tool-calling", AiTaskRouter.TOOL_CALLING, "default-model");
    }

    private void assertRoute(String taskType, String expectedTask, String expectedModel) {
        AiTaskRouter.ModelRoute route = router.route(new ModelContracts.ChatRequest(
                "question",
                List.of(),
                List.of(),
                taskType
        ));
        assertEquals(expectedTask, route.taskType());
        assertEquals(expectedModel, route.model());
    }

    private MatrixAiProperties properties() {
        MatrixAiProperties value = new MatrixAiProperties();
        value.setInternalToken("test-token");
        value.setModelName("default-model");
        value.setKnowledgeQaModel("knowledge-model");
        value.setFinancialAnalysisModel("analysis-model");
        value.setToolCallingModel("tool-model");
        value.setEvaluationModel("evaluation-model");
        return value;
    }
}
