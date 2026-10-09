package single.cjj.erp.sales.commercial.workflow;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import single.cjj.erp.sales.commercial.service.SalesCommercialService;
import single.cjj.erp.sales.commercial.workflow.mapper.SalesWorkflowLinkMapper;

import java.util.Map;
import java.util.Set;

@Service
@ConditionalOnProperty(prefix="matrix.sales.workflow", name="enabled", havingValue="true")
public class SalesWorkflowCallbackHandler {
    private final SalesWorkflowLinkMapper links;
    private final SalesCommercialService commercial;
    private final long systemActorId;

    public SalesWorkflowCallbackHandler(SalesWorkflowLinkMapper links, SalesCommercialService commercial,
            @Value("${matrix.sales.workflow.system-actor-id:0}") long systemActorId) {
        if (systemActorId <= 0) {
            throw new IllegalStateException("Workflow requires a dedicated positive system actor ID");
        }
        this.links=links; this.commercial=commercial; this.systemActorId=systemActorId;
    }

    @Transactional(rollbackFor=Exception.class)
    public void apply(String eventId,String eventType,WorkflowEvent event) {
        if(!Set.of("INSTANCE_COMPLETED","INSTANCE_REJECTED").contains(eventType)
           || !eventType.equals(event.eventType())
           || eventId==null || !eventId.equals(event.eventId())
           || event.instanceId()==null || event.businessId()==null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Workflow callback type or event ID invalid");
        }
        SalesWorkflowLinkEntity link=links.lockByInstance(event.instanceId());
        if(link==null) {
            // A callback may race with the asynchronous start/DB bind.
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Workflow instance not bound yet");
        }
        if(!link.getFtenantId().equals(event.tenantId())
                || !"MATRIX_ERP".equals(event.sourceSystem())
                || !link.getFdocumentType().equals(event.businessType())
                || !String.valueOf(link.getFdocumentId()).equals(event.businessId())
                || event.variables()==null
                || !String.valueOf(link.getForgId()).equals(
                        String.valueOf(event.variables().get("organizationId")))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Workflow callback does not match bound document");
        }
        String next="INSTANCE_COMPLETED".equals(eventType)?"APPROVED":"REJECTED";
        if(!"ACTIVE".equals(link.getFstatus())) {
            if(next.equals(link.getFstatus()) && eventId.equals(link.getFprocessedEventId()))return;
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Workflow was already finalized differently");
        }
        boolean approved="APPROVED".equals(next);
        if("SALES_QUOTE".equals(link.getFdocumentType())) {
            commercial.transitionQuoteFromWorkflow(link.getFdocumentId(),link.getFtenantId(),
                    approved,systemActorId);
        } else if("SALES_CONTRACT".equals(link.getFdocumentType())) {
            commercial.transitionContractFromWorkflow(link.getFdocumentId(),link.getFtenantId(),
                    approved,systemActorId);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid workflow business type");
        }
        if(links.finish(link.getFid(),next,eventId)!=1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Workflow callback was already processed");
        }
    }

    public record WorkflowEvent(String eventId,String eventType,String instanceId,
           String tenantId,String sourceSystem,String businessType,String businessId,
           String status,Map<String,Object> variables) {}
}
