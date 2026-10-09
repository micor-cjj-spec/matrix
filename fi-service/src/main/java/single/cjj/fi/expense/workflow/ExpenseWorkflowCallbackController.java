package single.cjj.fi.expense.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.bizfi.exception.BizException;

import java.util.Map;

@RestController
@RequestMapping("/fi/expense/workflow")
public class ExpenseWorkflowCallbackController {

    private final ExpenseWorkflowCallbackSigner signer;
    private final ExpenseWorkflowCallbackService callbackService;
    private final ObjectMapper objectMapper;

    public ExpenseWorkflowCallbackController(
            ExpenseWorkflowCallbackSigner signer,
            ExpenseWorkflowCallbackService callbackService,
            ObjectMapper objectMapper) {
        this.signer = signer;
        this.callbackService = callbackService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/events")
    public ApiResponse<Map<String, Object>> receive(
            @RequestHeader("X-Workflow-Event-Id") String eventId,
            @RequestHeader("X-Workflow-Timestamp") String timestamp,
            @RequestHeader("X-Workflow-Signature") String signature,
            @RequestBody String body) {
        signer.verify(timestamp, signature, body);
        try {
            ExpenseWorkflowContracts.WorkflowEventRequest event = objectMapper.readValue(
                    body, ExpenseWorkflowContracts.WorkflowEventRequest.class);
            if (!eventId.equals(event.eventId())) {
                throw new BizException("工作流事件 ID 与请求头不一致");
            }
            boolean processed = callbackService.process(event);
            return ApiResponse.success(Map.of(
                    "eventId", event.eventId(),
                    "processed", processed,
                    "duplicate", !processed
            ));
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException("工作流回调内容无法解析: " + ex.getMessage());
        }
    }
}
