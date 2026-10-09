package single.cjj.erp.sales.commercial.workflow;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import single.cjj.bizfi.exception.BizException;
import single.cjj.erp.sales.commercial.workflow.mapper.SalesWorkflowLinkMapper;

import java.time.LocalDateTime;
import java.util.Set;

@Service
public class SalesWorkflowCoordinator {
    private final SalesWorkflowLinkMapper links;
    private final boolean enabled;
    private final String quoteDefinition;
    private final String contractDefinition;

    public SalesWorkflowCoordinator(SalesWorkflowLinkMapper links,
          @Value("${matrix.sales.workflow.enabled:false}") boolean enabled,
          @Value("${matrix.sales.workflow.quote-definition:}") String quoteDefinition,
          @Value("${matrix.sales.workflow.contract-definition:}") String contractDefinition) {
        this.links=links;
        this.enabled=enabled;
        this.quoteDefinition=quoteDefinition;
        this.contractDefinition=contractDefinition;
        if (enabled && (quoteDefinition.isBlank() || contractDefinition.isBlank())) {
            throw new IllegalStateException("Sales Workflow requires published definition keys");
        }
    }

    public boolean enabled() { return enabled; }

    public record WorkflowStatus(String status, String instanceId, String definitionKey) {}

    public WorkflowStatus status(String tenant, String documentType, Long documentId) {
        if (!enabled) return new WorkflowStatus("NOT_ENABLED", null, null);
        SalesWorkflowLinkEntity link=links.findDocument(tenant, documentType, documentId);
        return link==null ? new WorkflowStatus("NOT_SUBMITTED",null,null)
                : new WorkflowStatus(link.getFstatus(),link.getFinstanceId(),link.getFdefinitionKey());
    }

    @Transactional(propagation=Propagation.MANDATORY)
    public void enqueue(String tenant, Long org, String type, Long documentId, Long initiator) {
        if (!enabled) return;
        if (!Set.of("SALES_QUOTE", "SALES_CONTRACT").contains(type)
                || org == null || documentId == null || initiator == null || initiator <= 0) {
            throw new BizException("销售流程关联参数不合法");
        }
        if (links.findDocument(tenant, type, documentId) != null) {
            throw new BizException("单据审批流程已经创建，不能重复提交");
        }
        SalesWorkflowLinkEntity link = new SalesWorkflowLinkEntity();
        link.setFid(IdWorker.getId());
        link.setFtenantId(tenant);
        link.setForgId(org);
        link.setFdocumentType(type);
        link.setFdocumentId(documentId);
        link.setFdefinitionKey("SALES_QUOTE".equals(type) ? quoteDefinition : contractDefinition);
        link.setFinitiatorId(initiator);
        // Stable on retry. Never use a timestamp or a caller-supplied value.
        link.setFidempotencyKey("ERP-SALES-" + tenant + "-" + type + "-" + documentId);
        link.setFstatus("PENDING");
        link.setFretryCount(0);
        link.setFcreateTime(LocalDateTime.now());
        link.setFmodifyTime(LocalDateTime.now());
        if (links.insert(link) != 1) {
            throw new BizException("销售流程启动任务创建失败");
        }
    }
}
