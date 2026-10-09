package single.cjj.erp.sales.commercial.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import single.cjj.bizfi.entity.ApiResponse;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Set;

@RestController
@RequestMapping("/internal/sales/workflow")
@ConditionalOnProperty(prefix="matrix.sales.workflow",name="enabled",havingValue="true")
public class SalesWorkflowCallbackController {
    private final ObjectMapper json;
    private final SalesWorkflowCallbackHandler handler;
    private final byte[] secret;

    public SalesWorkflowCallbackController(ObjectMapper json, SalesWorkflowCallbackHandler handler,
            @Value("${matrix.sales.workflow.callback-secret:}") String signingSecret){
        if(!StringUtils.hasText(signingSecret)
                || signingSecret.getBytes(StandardCharsets.UTF_8).length<32){
            throw new IllegalStateException("Workflow callback signing secret must contain at least 32 bytes");
        }
        this.json=json;this.handler=handler;
        this.secret=signingSecret.getBytes(StandardCharsets.UTF_8);
    }

    @PostMapping("/callback")
    public ApiResponse<String> callback(
         @RequestHeader("X-Workflow-Timestamp") String timestamp,
         @RequestHeader("X-Workflow-Signature") String signature,
         @RequestHeader("X-Workflow-Event-Id") String eventId,
         @RequestHeader("X-Workflow-Event-Type") String eventType,
         @RequestBody String rawBody){
        verify(timestamp,signature,rawBody);
        try {
            SalesWorkflowCallbackHandler.WorkflowEvent event=
                json.readValue(rawBody,SalesWorkflowCallbackHandler.WorkflowEvent.class);
            handler.apply(eventId,eventType,event);
            return ApiResponse.success("已处理");
        }catch(com.fasterxml.jackson.core.JsonProcessingException ex){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Workflow JSON 无效");
        }
    }

    private void verify(String timestamp,String signature,String body){
        if(!StringUtils.hasText(signature)||!signature.startsWith("sha256="))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"缺少有效的回调签名");
        try{
            long epoch=Long.parseLong(timestamp);
            if(Math.abs(Instant.now().getEpochSecond()-epoch)>300)
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"回调时间戳过期");
            Mac mac=Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret,"HmacSHA256"));
            String expected="sha256="+HexFormat.of().formatHex(mac.doFinal(
                    (timestamp+"."+body).getBytes(StandardCharsets.UTF_8)));
            if(!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8)))
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"回调签名无效");
        }catch(ResponseStatusException ex){throw ex;}
        catch(Exception ex){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"无法验证 Workflow 回调");
        }
    }
}
