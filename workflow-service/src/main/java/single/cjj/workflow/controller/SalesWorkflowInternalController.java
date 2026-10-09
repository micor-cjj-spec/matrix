package single.cjj.workflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.workflow.api.WorkflowContracts;
import single.cjj.workflow.service.WorkflowService;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Set;

/** ERP-only workflow start entrypoint. Public /workflow/instances rejects ERP sales starts. */
@RestController
@RequestMapping("/workflow/internal/sales")
@ConditionalOnProperty(prefix="matrix.sales.workflow", name="internal-enabled", havingValue="true")
public class SalesWorkflowInternalController {
    private static final Set<String> TYPES = Set.of("SALES_QUOTE", "SALES_CONTRACT");
    private final WorkflowService workflowService;
    private final ObjectMapper objectMapper;
    private final byte[] secret;
    private final String callbackUrl;

    public SalesWorkflowInternalController(
            WorkflowService workflowService, ObjectMapper objectMapper,
            @Value("${workflow.sales-internal.signing-secret:}") String signingSecret,
            @Value("${workflow.sales-internal.callback-url:}") String callbackUrl) {
        if (!StringUtils.hasText(signingSecret)
                || signingSecret.getBytes(StandardCharsets.UTF_8).length < 32
                || !StringUtils.hasText(callbackUrl)
                || !callbackUrl.startsWith("https://")) {
            throw new IllegalStateException("ERP sales workflow requires signing secret and HTTPS callback URL");
        }
        this.workflowService = workflowService;
        this.objectMapper = objectMapper;
        this.secret = signingSecret.getBytes(StandardCharsets.UTF_8);
        this.callbackUrl = callbackUrl;
    }

    @PostMapping("/instances")
    public ApiResponse<WorkflowContracts.InstanceResponse> start(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestHeader("X-Sales-Timestamp") String timestamp,
            @RequestHeader("X-Sales-Signature") String signature,
            @RequestBody String rawBody) {
        requireSignature(timestamp, signature, rawBody);
        try {
            WorkflowContracts.StartWorkflowRequest supplied =
                    objectMapper.readValue(rawBody, WorkflowContracts.StartWorkflowRequest.class);
            if (!"MATRIX_ERP".equals(supplied.sourceSystem())
                    || !TYPES.contains(supplied.businessType())
                    || !StringUtils.hasText(supplied.tenantId())
                    || !StringUtils.hasText(supplied.definitionKey())
                    || !StringUtils.hasText(supplied.initiatorId())
                    || !StringUtils.hasText(supplied.businessId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ERP sales workflow scope invalid");
            }
            // Never accept a callback URL supplied in a request, even when signed.
            WorkflowContracts.StartWorkflowRequest trusted = new WorkflowContracts.StartWorkflowRequest(
                    supplied.tenantId(), supplied.definitionKey(), supplied.sourceSystem(),
                    supplied.businessType(), supplied.businessId(), supplied.initiatorId(),
                    supplied.safeVariables(), callbackUrl);
            return ApiResponse.success(workflowService.startWorkflow(trusted, idempotencyKey));
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请求体不是有效的流程启动 JSON");
        }
    }

    private void requireSignature(String timestamp, String signature, String rawBody) {
        try {
            long epoch = Long.parseLong(timestamp);
            if (Math.abs(Instant.now().getEpochSecond() - epoch) > 300) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "流程启动请求过期");
            }
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(secret, "HmacSHA256"));
            String expected = "sha256=" + HexFormat.of().formatHex(
                    hmac.doFinal((timestamp + "." + rawBody).getBytes(StandardCharsets.UTF_8)));
            if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8))) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "流程启动请求签名无效");
            }
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "流程启动请求无法验证");
        }
    }
}
