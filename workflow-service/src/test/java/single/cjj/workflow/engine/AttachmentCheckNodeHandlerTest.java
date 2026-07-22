package single.cjj.workflow.engine;

import org.junit.jupiter.api.Test;
import single.cjj.workflow.attachment.WorkflowAttachmentRepository;
import single.cjj.workflow.model.WorkflowDefinition;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AttachmentCheckNodeHandlerTest {

    private final WorkflowAttachmentRepository repository = mock(WorkflowAttachmentRepository.class);
    private final AttachmentCheckNodeHandler handler = new AttachmentCheckNodeHandler(repository);

    @Test
    void shouldPassWhenEveryCategoryMeetsMinimumCount() {
        when(repository.countUploadedCategories("instance-1"))
                .thenReturn(Map.of("INVOICE", 2L, "PAYMENT_PROOF", 1L));

        WorkflowDefinition.Node node = nodeWithRequirements(List.of(
                Map.of("category", "INVOICE", "minimumCount", 2),
                "PAYMENT_PROOF"
        ));

        WorkflowNodeHandler.ExecutionResult result = handler.execute(
                new WorkflowNodeHandler.ExecutionContext("instance-1", node, Map.of()));

        assertTrue(result.success());
    }

    @Test
    void shouldReportMissingQuantity() {
        when(repository.countUploadedCategories("instance-1"))
                .thenReturn(Map.of("INVOICE", 1L));

        WorkflowDefinition.Node node = nodeWithRequirements(List.of(
                Map.of("categoryCode", "INVOICE", "minimumCount", 2),
                "PAYMENT_PROOF"
        ));

        WorkflowNodeHandler.ExecutionResult result = handler.execute(
                new WorkflowNodeHandler.ExecutionContext("instance-1", node, Map.of()));

        assertFalse(result.success());
        assertTrue(result.message().contains("INVOICE缺少1份"));
        assertTrue(result.message().contains("PAYMENT_PROOF缺少1份"));
    }

    private WorkflowDefinition.Node nodeWithRequirements(List<?> requirements) {
        WorkflowDefinition.Node node = new WorkflowDefinition.Node();
        node.setKey("attachmentCheck");
        node.setType(WorkflowDefinition.NodeType.SERVICE_TASK);
        node.setConfig(Map.of("requiredCategories", requirements));
        return node;
    }
}
