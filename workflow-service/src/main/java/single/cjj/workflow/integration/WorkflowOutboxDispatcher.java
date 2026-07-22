package single.cjj.workflow.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import single.cjj.workflow.repository.WorkflowOutboxRepository;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class WorkflowOutboxDispatcher {

    private final WorkflowOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final RestClient restClient = RestClient.create();

    @Value("${workflow.outbox.batch-size:20}")
    private int batchSize;

    public WorkflowOutboxDispatcher(WorkflowOutboxRepository outboxRepository,
                                    ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${workflow.outbox.dispatch-delay-ms:5000}")
    public void dispatch() {
        List<WorkflowOutboxRepository.OutboxRow> rows = outboxRepository
                .findDispatchable(Math.max(1, batchSize));
        for (WorkflowOutboxRepository.OutboxRow row : rows) {
            if (!outboxRepository.claim(row.id())) {
                continue;
            }
            dispatchOne(row);
        }
    }

    private void dispatchOne(WorkflowOutboxRepository.OutboxRow row) {
        try {
            Map<String, Object> payload = objectMapper.readValue(
                    row.payloadJson(),
                    new TypeReference<LinkedHashMap<String, Object>>() {
                    }
            );
            String callbackUrl = payload.get("callbackUrl") == null
                    ? null : String.valueOf(payload.get("callbackUrl"));
            if (!StringUtils.hasText(callbackUrl)) {
                outboxRepository.markSent(row.id());
                return;
            }

            restClient.post()
                    .uri(callbackUrl)
                    .header("X-Workflow-Event-Id", row.eventId())
                    .header("X-Workflow-Event-Type", row.eventType())
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            outboxRepository.markSent(row.id());
        } catch (Exception ex) {
            long delaySeconds = Math.min(3600L, 5L * (1L << Math.min(row.retryCount(), 9)));
            LocalDateTime nextRetryTime = LocalDateTime.now().plusSeconds(delaySeconds);
            outboxRepository.markFailed(row.id(), nextRetryTime);
            log.warn("workflow callback failed, eventId={}, nextRetryTime={}",
                    row.eventId(), nextRetryTime, ex);
        }
    }
}
