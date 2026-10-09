package single.cjj.erp.sales.commercial.workflow;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import single.cjj.erp.sales.commercial.service.SalesCommercialService;
import single.cjj.erp.sales.commercial.workflow.mapper.SalesWorkflowLinkMapper;

import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SalesWorkflowCallbackHandlerTest {
    private final SalesWorkflowLinkMapper links=mock(SalesWorkflowLinkMapper.class);
    private final SalesCommercialService commercial=mock(SalesCommercialService.class);
    private final SalesWorkflowCallbackHandler handler =
        new SalesWorkflowCallbackHandler(links,commercial,9999L);

    private SalesWorkflowLinkEntity link(){
        SalesWorkflowLinkEntity v=new SalesWorkflowLinkEntity();
        v.setFid(12L);v.setFtenantId("T1");v.setForgId(3L);
        v.setFdocumentType("SALES_QUOTE");v.setFdocumentId(100L);
        v.setFinstanceId("wf-001");v.setFstatus("ACTIVE");
        return v;
    }

    private SalesWorkflowCallbackHandler.WorkflowEvent event(String type){
        return new SalesWorkflowCallbackHandler.WorkflowEvent("evt001",type,"wf-001",
                "T1","MATRIX_ERP","SALES_QUOTE","100",
                "INSTANCE_COMPLETED".equals(type)?"COMPLETED":"REJECTED",
                Map.of("organizationId","3"));
    }

    @Test
    void completedWorkflowApprovesBoundQuoteAndMarksLink() {
        when(links.lockByInstance("wf-001")).thenReturn(link());
        when(links.finish(12L,"APPROVED","evt001")).thenReturn(1);
        handler.apply("evt001","INSTANCE_COMPLETED",event("INSTANCE_COMPLETED"));
        verify(commercial).transitionQuoteFromWorkflow(100L,"T1",true,9999L);
        verify(links).finish(12L,"APPROVED","evt001");
    }

    @Test
    void rejectedWorkflowDoesNotApprove() {
        when(links.lockByInstance("wf-001")).thenReturn(link());
        when(links.finish(12L,"REJECTED","evt001")).thenReturn(1);
        handler.apply("evt001","INSTANCE_REJECTED",event("INSTANCE_REJECTED"));
        verify(commercial).transitionQuoteFromWorkflow(100L,"T1",false,9999L);
    }

    @Test
    void eventRetryIsIdempotentAfterCommit() {
        var link=link();link.setFstatus("APPROVED");link.setFprocessedEventId("evt001");
        when(links.lockByInstance("wf-001")).thenReturn(link);
        handler.apply("evt001","INSTANCE_COMPLETED",event("INSTANCE_COMPLETED"));
        verifyNoInteractions(commercial);verify(links,never()).finish(anyLong(),anyString(),anyString());
    }

    @Test
    void conflictingReplayIsRejected() {
        var link=link();link.setFstatus("APPROVED");link.setFprocessedEventId("evt001");
        when(links.lockByInstance("wf-001")).thenReturn(link);
        var other=new SalesWorkflowCallbackHandler.WorkflowEvent("evt002","INSTANCE_REJECTED",
            "wf-001","T1","MATRIX_ERP","SALES_QUOTE","100","REJECTED",Map.of("organizationId","3"));
        assertEquals(409,code(()->handler.apply("evt002","INSTANCE_REJECTED",other)));
        verifyNoInteractions(commercial);
    }

    @Test
    void rejectsForeignTenantAndDocument() {
        when(links.lockByInstance("wf-001")).thenReturn(link());
        var forged=new SalesWorkflowCallbackHandler.WorkflowEvent("evt001","INSTANCE_COMPLETED",
            "wf-001","OTHER","MATRIX_ERP","SALES_QUOTE","100","COMPLETED",Map.of("organizationId","3"));
        assertEquals(403,code(()->handler.apply("evt001","INSTANCE_COMPLETED",forged)));
        verifyNoInteractions(commercial);
    }

    @Test
    void unboundInstanceMustRetryLater() {
        assertEquals(409,code(()->handler.apply("evt001","INSTANCE_COMPLETED",event("INSTANCE_COMPLETED"))));
        verifyNoInteractions(commercial);
    }

    private int code(Runnable action) {
        return assertThrows(ResponseStatusException.class,action::run).getStatusCode().value();
    }
}
