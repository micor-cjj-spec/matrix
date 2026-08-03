package single.cjj.matrix.ai.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "matrix.ai")
public class MatrixAiProperties {

    @NotBlank
    private String internalToken;

    private String systemPrompt = "你是 Matrix 企业财务平台的 AI 助手。默认使用中文回答，优先依据提供的业务知识回答；不知道时明确说明，不要编造系统能力或业务数据。";

    private String modelName = "gpt-4o-mini";
    private String embeddingModelName = "text-embedding-3-small";
    private String knowledgeQaModel;
    private String financialAnalysisModel;
    private String toolCallingModel;
    private String evaluationModel;

    private String financeToolBaseUrl = "http://127.0.0.1:10003/api";
    private String financeToolInternalToken;
    private Integer financeToolTimeoutSeconds = 20;

    public String getInternalToken() {
        return internalToken;
    }

    public void setInternalToken(String internalToken) {
        this.internalToken = internalToken;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getEmbeddingModelName() {
        return embeddingModelName;
    }

    public void setEmbeddingModelName(String embeddingModelName) {
        this.embeddingModelName = embeddingModelName;
    }

    public String getKnowledgeQaModel() {
        return knowledgeQaModel;
    }

    public void setKnowledgeQaModel(String knowledgeQaModel) {
        this.knowledgeQaModel = knowledgeQaModel;
    }

    public String getFinancialAnalysisModel() {
        return financialAnalysisModel;
    }

    public void setFinancialAnalysisModel(String financialAnalysisModel) {
        this.financialAnalysisModel = financialAnalysisModel;
    }

    public String getToolCallingModel() {
        return toolCallingModel;
    }

    public void setToolCallingModel(String toolCallingModel) {
        this.toolCallingModel = toolCallingModel;
    }

    public String getEvaluationModel() {
        return evaluationModel;
    }

    public void setEvaluationModel(String evaluationModel) {
        this.evaluationModel = evaluationModel;
    }

    public String getFinanceToolBaseUrl() {
        return financeToolBaseUrl;
    }

    public void setFinanceToolBaseUrl(String financeToolBaseUrl) {
        this.financeToolBaseUrl = financeToolBaseUrl;
    }

    public String getFinanceToolInternalToken() {
        return financeToolInternalToken;
    }

    public void setFinanceToolInternalToken(String financeToolInternalToken) {
        this.financeToolInternalToken = financeToolInternalToken;
    }

    public Integer getFinanceToolTimeoutSeconds() {
        return financeToolTimeoutSeconds;
    }

    public void setFinanceToolTimeoutSeconds(Integer financeToolTimeoutSeconds) {
        this.financeToolTimeoutSeconds = financeToolTimeoutSeconds;
    }
}
