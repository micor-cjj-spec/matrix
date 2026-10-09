package single.cjj.workflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import single.cjj.workflow.api.WorkflowContracts;
import single.cjj.workflow.service.WorkflowService;
import single.cjj.workflow.service.WorkflowHistoryService;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SalesWorkflowInternalControllerTest {
    private static final String SECRET="matrix-test-signing-key-32-bytes-minimum-only";
    private final WorkflowService service=mock(WorkflowService.class);
    private final SalesWorkflowInternalController controller=new SalesWorkflowInternalController(
            service,new ObjectMapper(),SECRET,"https://erp.internal/api/internal/sales/workflow/callback",SECRET);

    private String sign(String timestamp,String body) throws Exception {
        Mac mac=Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));
        return "sha256="+HexFormat.of().formatHex(mac.doFinal(
                (timestamp+"."+body).getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void signedInternalStartOverridesAnyRequestedCallbackUrl() throws Exception {
        String body="{\"tenantId\":\"T1\",\"definitionKey\":\"quote_v1\","
                +"\"sourceSystem\":\"MATRIX_ERP\",\"businessType\":\"SALES_QUOTE\","
                +"\"businessId\":\"100\",\"initiatorId\":\"7\","
                +"\"variables\":{\"organizationId\":\"3\"},"
                +"\"callbackUrl\":\"https://attacker.example/callback\"}";
        String time=Long.toString(Instant.now().getEpochSecond());
        controller.start("idem-100",time,sign(time,body),body);
        org.mockito.ArgumentCaptor<WorkflowContracts.StartWorkflowRequest> capture=
                org.mockito.ArgumentCaptor.forClass(WorkflowContracts.StartWorkflowRequest.class);
        verify(service).startWorkflow(capture.capture(),eq("idem-100"));
        assertEquals("https://erp.internal/api/internal/sales/workflow/callback",
                capture.getValue().callbackUrl());
    }

    @Test
    void forgedOrOldRequestsAreRejected() throws Exception {
        String time=Long.toString(Instant.now().getEpochSecond());
        assertThrows(ResponseStatusException.class,()->
                controller.start("idem",time,"sha256=forged","{}"));
        String old=Long.toString(Instant.now().minusSeconds(900).getEpochSecond());
        assertThrows(ResponseStatusException.class,()->
                controller.start("idem",old,sign(old,"{}"),"{}"));
        verifyNoInteractions(service);
    }

    @Test
    void publicInstanceEndpointCannotInitiateErpSales() {
        WorkflowInstanceController publicController=new WorkflowInstanceController(
                service,mock(WorkflowHistoryService.class));
        var req=new WorkflowContracts.StartWorkflowRequest("T1","quote_v1",
                "MATRIX_ERP","SALES_QUOTE","100","7",java.util.Map.of(),null);
        assertThrows(ResponseStatusException.class,()->
                publicController.start("idem",req));
        verifyNoInteractions(service);
    }
}
