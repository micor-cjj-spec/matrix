package single.cjj.erp.sales.commercial.workflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import single.cjj.erp.sales.commercial.workflow.mapper.SalesWorkflowLinkMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/** Durable, claim-based Workflow start. No network call occurs inside the sales document transaction. */
@Component
@ConditionalOnProperty(prefix="matrix.sales.workflow", name="enabled", havingValue="true")
public class SalesWorkflowStartDispatcher {
    private final SalesWorkflowLinkMapper links;
    private final ObjectMapper json;
    private final RestClient http;
    private final String endpoint;
    private final byte[] secret;

    public SalesWorkflowStartDispatcher(SalesWorkflowLinkMapper links, ObjectMapper json,
            @Value("${matrix.sales.workflow.start-url:}") String endpoint,
            @Value("${matrix.sales.workflow.internal-secret:}") String signingSecret) {
        if (!endpoint.startsWith("https://")
                || signingSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("Workflow start requires HTTPS endpoint and strong internal signing secret");
        }
        this.links=links; this.json=json; this.endpoint=endpoint;
        this.secret=signingSecret.getBytes(StandardCharsets.UTF_8);
        this.http=RestClient.create();
    }

    @Scheduled(fixedDelayString="${matrix.sales.workflow.dispatch-delay-ms:5000}")
    public void dispatch() {
        links.recoverStale();
        List<SalesWorkflowLinkEntity> pending=links.ready(20);
        for (SalesWorkflowLinkEntity link:pending) {
            if (links.claim(link.getFid()) != 1) continue;
            startOne(link);
        }
    }

    private void startOne(SalesWorkflowLinkEntity link) {
        try {
            String body=json.writeValueAsString(Map.of(
                "tenantId",link.getFtenantId(),
                "definitionKey",link.getFdefinitionKey(),
                "sourceSystem","MATRIX_ERP",
                "businessType",link.getFdocumentType(),
                "businessId",String.valueOf(link.getFdocumentId()),
                "initiatorId",String.valueOf(link.getFinitiatorId()),
                "variables",Map.of("organizationId",String.valueOf(link.getForgId()),
                        "documentId",String.valueOf(link.getFdocumentId()))));
            long timestamp=Instant.now().getEpochSecond();
            String signature=sign(timestamp,body);
            String response=http.post().uri(endpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Idempotency-Key",link.getFidempotencyKey())
                    .header("X-Sales-Timestamp",String.valueOf(timestamp))
                    .header("X-Sales-Signature",signature)
                    .body(body).retrieve().body(String.class);
            JsonNode root=json.readTree(response);
            if(root.path("code").asInt()!=200
                    || root.path("data").path("instanceId").asText().isBlank()){
                throw new IllegalStateException("Workflow start did not return an instance");
            }
            String instanceId=root.path("data").path("instanceId").asText();
            if(links.activated(link.getFid(),instanceId)!=1){
                throw new IllegalStateException("Unable to bind workflow instance");
            }
        } catch(Exception ex){
            int attempts=link.getFretryCount()==null?0:link.getFretryCount();
            long backoff=Math.min(3600L,5L*(1L<<Math.min(attempts,9)));
            links.failed(link.getFid(),LocalDateTime.now().plusSeconds(backoff));
            // Do not log bearer credentials, signing secret or full request body.
            org.slf4j.LoggerFactory.getLogger(getClass()).warn(
                    "Sales workflow start will retry, linkId={}, retryCount={}",
                    link.getFid(),attempts+1);
        }
    }

    private String sign(long timestamp,String body) throws Exception {
        Mac mac=Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret,"HmacSHA256"));
        return "sha256="+HexFormat.of().formatHex(mac.doFinal(
                (timestamp+"."+body).getBytes(StandardCharsets.UTF_8)));
    }
}
