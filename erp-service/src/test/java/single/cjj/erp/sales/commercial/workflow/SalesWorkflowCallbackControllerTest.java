package single.cjj.erp.sales.commercial.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SalesWorkflowCallbackControllerTest {
    private static final String SECRET="matrix-test-signing-key-32-bytes-minimum-only";
    private final SalesWorkflowCallbackHandler handler=mock(SalesWorkflowCallbackHandler.class);
    private final SalesWorkflowCallbackController controller=
        new SalesWorkflowCallbackController(new ObjectMapper(),handler,SECRET);

    private String sign(String timestamp,String body) throws Exception {
        Mac m=Mac.getInstance("HmacSHA256");
        m.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));
        return "sha256="+HexFormat.of().formatHex(m.doFinal(
            (timestamp+"."+body).getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void validSignedCallbackReachesApplicationService() throws Exception {
        String body="{\"eventId\":\"evt1\",\"eventType\":\"INSTANCE_COMPLETED\","
                + "\"instanceId\":\"wf1\",\"tenantId\":\"T1\",\"sourceSystem\":\"MATRIX_ERP\","
                + "\"businessType\":\"SALES_QUOTE\",\"businessId\":\"100\","
                + "\"status\":\"COMPLETED\",\"variables\":{\"organizationId\":\"3\"}}";
        String t=Long.toString(Instant.now().getEpochSecond());
        controller.callback(t,sign(t,body),"evt1","INSTANCE_COMPLETED",body);
        verify(handler).apply(eq("evt1"),eq("INSTANCE_COMPLETED"),any());
    }

    @Test
    void invalidSignatureOrOldTimestampIsRejectedBeforeHandler() throws Exception {
        String t=Long.toString(Instant.now().getEpochSecond());
        assertThrows(ResponseStatusException.class,()->
            controller.callback(t,"sha256=wrong","evt1","INSTANCE_COMPLETED","{}"));
        String old=Long.toString(Instant.now().minusSeconds(600).getEpochSecond());
        assertThrows(ResponseStatusException.class,()->
            controller.callback(old,sign(old,"{}"),"evt1","INSTANCE_COMPLETED","{}"));
        verifyNoInteractions(handler);
    }
}
