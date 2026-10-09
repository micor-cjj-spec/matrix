package single.cjj.fi.expense.workflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import single.cjj.bizfi.exception.BizException;

import java.util.Locale;

@Component
public class ExpenseWorkflowGateway {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public ExpenseWorkflowGateway(
            ObjectMapper objectMapper,
            @Value("${fi.workflow.base-url:http://localhost:10006/api}") String baseUrl) {
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().baseUrl(baseUrl.replaceAll("/+$", "")).build();
    }

    public WorkflowResult start(String payloadJson, String idempotencyKey) {
        String response = restClient.post()
                .uri("/workflow/instances")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .body(payloadJson)
                .retrieve()
                .body(String.class);
        return parseResult(response);
    }

    public WorkflowResult resubmit(String instanceId, String payloadJson, String requestId) {
        String response = restClient.post()
                .uri("/workflow/instances/{instanceId}/resubmit", instanceId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Request-Id", requestId)
                .body(payloadJson)
                .retrieve()
                .body(String.class);
        return parseResult(response);
    }

    public WorkflowResult cancel(String instanceId, String payloadJson, String requestId) {
        String response = restClient.post()
                .uri("/workflow/instances/{instanceId}/cancel", instanceId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Request-Id", requestId)
                .body(payloadJson)
                .retrieve()
                .body(String.class);
        return parseResult(response);
    }

    public JsonNode getBusinessInstance(String tenantId, String businessId) {
        return getData("/workflow/business/" + ExpenseWorkflowService.SOURCE_SYSTEM + "/"
                + ExpenseWorkflowService.BUSINESS_TYPE + "/" + businessId + "?tenantId=" + tenantId);
    }

    public JsonNode getInstance(String instanceId) {
        return getData("/workflow/instances/" + instanceId);
    }

    public JsonNode listTasks(String instanceId) {
        return getData("/workflow/instances/" + instanceId + "/tasks");
    }

    public JsonNode listTimeline(String instanceId) {
        return getData("/workflow/instances/" + instanceId + "/timeline");
    }

    public JsonNode listAttachments(String tenantId, String businessId) {
        return getData("/workflow/files/business/" + ExpenseWorkflowService.SOURCE_SYSTEM + "/"
                + ExpenseWorkflowService.BUSINESS_TYPE + "/" + businessId + "?tenantId=" + tenantId);
    }

    public void requireUploadedCategory(String tenantId,
                                        String businessId,
                                        String categoryCode,
                                        int minimumCount) {
        JsonNode attachments = listAttachments(tenantId, businessId);
        int count = 0;
        if (attachments != null && attachments.isArray()) {
            for (JsonNode attachment : attachments) {
                String category = attachment.path("categoryCode").asText("")
                        .toUpperCase(Locale.ROOT);
                if (categoryCode.equalsIgnoreCase(category)
                        && "UPLOADED".equalsIgnoreCase(attachment.path("uploadStatus").asText())
                        && "CLEAN".equalsIgnoreCase(attachment.path("scanStatus").asText())) {
                    count++;
                }
            }
        }
        if (count < minimumCount) {
            throw new BizException("提交审批前至少需要 " + minimumCount + " 份 " + categoryCode + " 影像");
        }
    }

    private JsonNode getData(String uri) {
        String response = restClient.get().uri(uri).retrieve().body(String.class);
        return parseData(response);
    }

    private WorkflowResult parseResult(String response) {
        JsonNode data = parseData(response);
        String instanceId = data.path("instanceId").asText();
        if (instanceId.isBlank()) {
            throw new BizException("工作流服务响应缺少 instanceId");
        }
        return new WorkflowResult(instanceId, data.path("status").asText("RUNNING"));
    }

    private JsonNode parseData(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            if (root == null || root.path("code").asInt() != 200) {
                throw new BizException("工作流服务调用失败: "
                        + (root == null ? "空响应" : root.path("message").asText("未知错误")));
            }
            return root.path("data");
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException("工作流服务响应无法解析: " + ex.getMessage());
        }
    }

    public record WorkflowResult(String instanceId, String status) {
    }
}
