package single.cjj.workflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import single.cjj.workflow.api.WorkflowContracts;
import single.cjj.workflow.engine.WorkflowConditionEvaluator;
import single.cjj.workflow.engine.WorkflowNodeHandlerRegistry;
import single.cjj.workflow.repository.WorkflowRepository;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceLifecycleTest {

    private static final String DEFINITION_JSON = """
            {
              "nodes": [
                {"key":"start","name":"开始","type":"START"},
                {"key":"firstReview","name":"初审","type":"USER_TASK",
                 "assigneeRule":{"type":"USER","value":"reviewer-1"}},
                {"key":"end","name":"结束","type":"END"}
              ],
              "transitions": [
                {"from":"start","to":"firstReview","priority":0},
                {"from":"firstReview","to":"end","priority":0}
              ]
            }
            """;

    @Mock
    private WorkflowRepository repository;
    @Mock
    private WorkflowConditionEvaluator conditionEvaluator;
    @Mock
    private WorkflowNodeHandlerRegistry handlerRegistry;

    private WorkflowService workflowService;

    @BeforeEach
    void setUp() {
        workflowService = new WorkflowService(
                repository, conditionEvaluator, handlerRegistry, new ObjectMapper()
        );
    }

    @Test
    void returnToInitiatorCreatesResubmitTask() {
        WorkflowRepository.TaskRow reviewTask = task(
                "task-1", "instance-1", "node-1", "firstReview", "reviewer-1", "PENDING", 0
        );
        WorkflowRepository.InstanceRow running = instance(
                "instance-1", "RUNNING", "firstReview", "{}", 3
        );
        WorkflowRepository.InstanceRow waiting = instance(
                "instance-1", "WAITING_RESUBMIT", "__RESUBMIT__",
                "{\"_workflowResumeNodeKey\":\"firstReview\"}", 4
        );

        when(repository.findTask("task-1")).thenReturn(Optional.of(reviewTask));
        when(repository.findInstance("instance-1")).thenReturn(Optional.of(running), Optional.of(waiting));
        when(repository.findDefinition("tenant-1", "expense", 1))
                .thenReturn(Optional.of(definitionRow()));
        when(repository.completeTask("task-1", 0, "RETURNED")).thenReturn(1);
        when(repository.returnInstanceToInitiator(anyString(), anyInt(), anyString())).thenReturn(1);

        WorkflowContracts.InstanceResponse response = workflowService.actOnTask(
                "task-1",
                new WorkflowContracts.TaskActionRequest(
                        WorkflowContracts.TaskAction.RETURN_TO_INITIATOR,
                        "reviewer-1",
                        "影像不清晰",
                        Map.of()
                ),
                "request-return-1"
        );

        ArgumentCaptor<WorkflowRepository.TaskRow> taskCaptor =
                ArgumentCaptor.forClass(WorkflowRepository.TaskRow.class);
        verify(repository).insertTask(taskCaptor.capture());
        assertThat(taskCaptor.getValue().nodeKey()).isEqualTo("__RESUBMIT__");
        assertThat(taskCaptor.getValue().assigneeType()).isEqualTo("USER");
        assertThat(taskCaptor.getValue().assigneeValue()).isEqualTo("initiator-1");
        assertThat(response.status()).isEqualTo("WAITING_RESUBMIT");
        verify(repository).insertOutbox(anyString(), anyString(), anyString(),
                org.mockito.ArgumentMatchers.eq("INSTANCE_RETURNED"), anyString());
    }

    @Test
    void resubmitRecreatesOriginalApprovalTask() {
        WorkflowRepository.InstanceRow waiting = instance(
                "instance-1", "WAITING_RESUBMIT", "__RESUBMIT__",
                "{\"_workflowResumeNodeKey\":\"firstReview\"}", 4
        );
        WorkflowRepository.InstanceRow running = instance(
                "instance-1", "RUNNING", "firstReview", "{\"amount\":12000}", 6
        );
        WorkflowRepository.TaskRow resubmitTask = task(
                "task-resubmit", "instance-1", "node-resubmit", "__RESUBMIT__",
                "initiator-1", "PENDING", 0
        );

        when(repository.findInstance("instance-1")).thenReturn(Optional.of(waiting), Optional.of(running));
        when(repository.findOpenTaskByInstanceAndNode("instance-1", "__RESUBMIT__"))
                .thenReturn(Optional.of(resubmitTask));
        when(repository.completeTask("task-resubmit", 0, "RESUBMITTED")).thenReturn(1);
        when(repository.findDefinition("tenant-1", "expense", 1))
                .thenReturn(Optional.of(definitionRow()));
        when(repository.resumeInstance(anyString(), anyInt(), anyString(), anyString())).thenReturn(1);

        WorkflowContracts.InstanceResponse response = workflowService.resubmitInstance(
                "instance-1",
                new WorkflowContracts.ResubmitInstanceRequest(
                        "initiator-1", "已补充影像", Map.of("amount", 12000)
                ),
                "request-resubmit-1"
        );

        ArgumentCaptor<WorkflowRepository.TaskRow> taskCaptor =
                ArgumentCaptor.forClass(WorkflowRepository.TaskRow.class);
        verify(repository).insertTask(taskCaptor.capture());
        assertThat(taskCaptor.getValue().nodeKey()).isEqualTo("firstReview");
        assertThat(taskCaptor.getValue().assigneeValue()).isEqualTo("reviewer-1");
        assertThat(response.status()).isEqualTo("RUNNING");
        verify(repository).insertOutbox(anyString(), anyString(), anyString(),
                org.mockito.ArgumentMatchers.eq("INSTANCE_RESUBMITTED"), anyString());
    }

    @Test
    void initiatorCanCancelRunningInstance() {
        WorkflowRepository.InstanceRow running = instance(
                "instance-1", "RUNNING", "firstReview", "{}", 3
        );
        WorkflowRepository.InstanceRow cancelled = instance(
                "instance-1", "CANCELLED", null,
                "{\"cancelReason\":\"业务单据作废\"}", 4
        );

        when(repository.findInstance("instance-1")).thenReturn(Optional.of(running), Optional.of(cancelled));
        when(repository.cancelInstance(anyString(), anyInt(), anyString())).thenReturn(1);

        WorkflowContracts.InstanceResponse response = workflowService.cancelInstance(
                "instance-1",
                new WorkflowContracts.CancelInstanceRequest("initiator-1", "业务单据作废"),
                "request-cancel-1"
        );

        assertThat(response.status()).isEqualTo("CANCELLED");
        verify(repository).cancelOpenTasks("instance-1");
        verify(repository).cancelActiveNodes("instance-1");
        verify(repository).insertOutbox(anyString(), anyString(), anyString(),
                org.mockito.ArgumentMatchers.eq("INSTANCE_CANCELLED"), anyString());
    }

    private WorkflowRepository.DefinitionVersionRow definitionRow() {
        return new WorkflowRepository.DefinitionVersionRow(
                "tenant-1", "expense", "费用报销", 1,
                DEFINITION_JSON, "PUBLISHED", LocalDateTime.now(), LocalDateTime.now()
        );
    }

    private WorkflowRepository.InstanceRow instance(String id,
                                                      String status,
                                                      String currentNode,
                                                      String variables,
                                                      int version) {
        return new WorkflowRepository.InstanceRow(
                id, "tenant-1", "expense", 1,
                "fi-service", "expense_claim", "EXP-1", "fi-service:expense_claim:EXP-1",
                "idem-1", "initiator-1", currentNode, status, variables,
                "https://example.test/callback", version, LocalDateTime.now(), null
        );
    }

    private WorkflowRepository.TaskRow task(String id,
                                            String instanceId,
                                            String nodeInstanceId,
                                            String nodeKey,
                                            String assignee,
                                            String status,
                                            int version) {
        return new WorkflowRepository.TaskRow(
                id, "tenant-1", instanceId, nodeInstanceId, nodeKey,
                nodeKey, "USER", assignee, status, version, LocalDateTime.now(), null
        );
    }
}
