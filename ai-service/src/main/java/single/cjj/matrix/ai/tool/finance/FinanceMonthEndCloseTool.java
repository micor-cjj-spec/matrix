package single.cjj.matrix.ai.tool.finance;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import single.cjj.matrix.ai.api.ModelContracts;
import single.cjj.matrix.ai.observability.AiToolMetrics;

import java.util.Map;

@Component
public class FinanceMonthEndCloseTool {

    public static final String TOOL_NAME = "monthEndCloseCheck";
    public static final String CONTEXT_TOOL_NAME = "month-end-close-check";

    private final FinanceToolClient client;
    private final AiToolMetrics metrics;

    public FinanceMonthEndCloseTool(FinanceToolClient client, AiToolMetrics metrics) {
        this.client = client;
        this.metrics = metrics;
    }

    @Tool(
            name = TOOL_NAME,
            description = "Run the read-only month-end close readiness check for the organization and accounting period already authorized by the server. The tool cannot post vouchers, approve documents, close periods, or change finance data. Call it when the user asks for month-end blockers, unposted vouchers, period readiness, or close recommendations."
    )
    public FinanceMonthEndCloseResult monthEndCloseCheck(ToolContext toolContext) {
        long startedAt = metrics.start();
        try {
            ModelContracts.ToolContext context = readContext(toolContext);
            FinanceMonthEndCloseResult result = client.monthEndCloseCheck(context);
            metrics.record(CONTEXT_TOOL_NAME, "success", startedAt);
            return result;
        } catch (RuntimeException failure) {
            metrics.record(CONTEXT_TOOL_NAME, "failure", startedAt);
            throw failure;
        }
    }

    private ModelContracts.ToolContext readContext(ToolContext toolContext) {
        Map<String, Object> values = toolContext == null ? null : toolContext.getContext();
        if (values == null || values.isEmpty()) {
            throw new IllegalStateException("month-end tool 缺少服务端 ToolContext");
        }
        String toolName = stringValue(values.get("toolName"));
        if (!CONTEXT_TOOL_NAME.equals(toolName)) {
            throw new IllegalStateException("month-end tool 未获得授权");
        }
        return new ModelContracts.ToolContext(
                toolName,
                longValue(values.get("requestedByUserId"), "requestedByUserId"),
                longValue(values.get("organizationId"), "organizationId"),
                requiredString(values.get("period"), "period"),
                requiredString(values.get("requestId"), "requestId"),
                requiredString(values.get("conversationId"), "conversationId"),
                requiredString(values.get("modelName"), "modelName"),
                requiredString(values.get("modelTraceId"), "modelTraceId")
        );
    }

    private Long longValue(Object value, String field) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value != null) {
            try {
                return Long.valueOf(value.toString());
            } catch (NumberFormatException ignored) {
                // handled below
            }
        }
        throw new IllegalStateException(field + " 无效");
    }

    private String requiredString(Object value, String field) {
        String text = stringValue(value);
        if (!StringUtils.hasText(text)) {
            throw new IllegalStateException(field + " 无效");
        }
        return text;
    }

    private String stringValue(Object value) {
        String text = value == null ? null : value.toString();
        return StringUtils.hasText(text) ? text.trim() : null;
    }
}
