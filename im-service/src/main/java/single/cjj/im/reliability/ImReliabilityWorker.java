package single.cjj.im.reliability;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import single.cjj.im.config.ImRabbitConfiguration;
import single.cjj.im.service.ChannelDispatchService;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Component
public class ImReliabilityWorker {

    private final ImOperationsRepository operationsRepository;
    private final ChannelDispatchService dispatchService;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private final int batchSize;
    private final int outboxTimeoutMinutes;

    public ImReliabilityWorker(ImOperationsRepository operationsRepository,
                               ChannelDispatchService dispatchService,
                               ObjectMapper objectMapper,
                               TransactionTemplate transactionTemplate,
                               @Value("${im.reliability.batch-size:100}") int batchSize,
                               @Value("${im.outbox.processing-timeout-minutes:5}") int outboxTimeoutMinutes) {
        this.operationsRepository = operationsRepository;
        this.dispatchService = dispatchService;
        this.objectMapper = objectMapper;
        this.transactionTemplate = transactionTemplate;
        this.batchSize = Math.max(1, Math.min(batchSize, 500));
        this.outboxTimeoutMinutes = Math.max(1, outboxTimeoutMinutes);
    }

    @Scheduled(fixedDelayString = "${im.outbox.recovery-delay-ms:60000}")
    public void recoverStaleOutbox() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = now.minusMinutes(outboxTimeoutMinutes);
        for (String id : operationsRepository.findStaleOutboxIds(deadline, batchSize)) {
            operationsRepository.recoverStaleOutbox(id, now.plusMinutes(1), now);
        }
    }

    @Scheduled(fixedDelayString = "${im.reliability.reconcile-delay-ms:60000}")
    public void reconcileMessageState() {
        for (String messageId : operationsRepository.findAggregateMismatchMessageIds(batchSize)) {
            dispatchService.refreshMessageStatus(messageId);
        }
        for (String channelTaskId : operationsRepository.findMissingLocalNotificationTaskIds(batchSize)) {
            repairMissingLocalNotification(channelTaskId);
        }
    }

    @RabbitListener(queues = ImRabbitConfiguration.CHANNEL_TASK_DLQ)
    public void persistDeadLetter(String payload, Channel channel, Message message) throws Exception {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            String messageId = message.getMessageProperties().getMessageId();
            if (messageId == null || messageId.isBlank()) {
                messageId = UUID.nameUUIDFromBytes(payload.getBytes(StandardCharsets.UTF_8)).toString();
            }
            Object reasonHeader = message.getMessageProperties().getHeaders().get("x-first-death-reason");
            String reason = reasonHeader == null ? "REJECTED_OR_NOT_REQUEUED" : String.valueOf(reasonHeader);
            operationsRepository.insertDeadLetter(
                    UUID.randomUUID().toString().replace("-", ""),
                    ImRabbitConfiguration.CHANNEL_TASK_QUEUE,
                    messageId,
                    payload,
                    reason,
                    LocalDateTime.now()
            );
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            channel.basicNack(deliveryTag, false, true);
        }
    }

    private void repairMissingLocalNotification(String channelTaskId) {
        LocalDateTime now = LocalDateTime.now();
        transactionTemplate.executeWithoutResult(status -> {
            if (operationsRepository.requeueMissingLocalTask(channelTaskId, now) == 0) {
                return;
            }
            try {
                String payload = objectMapper.writeValueAsString(Map.of("channelTaskId", channelTaskId));
                operationsRepository.insertRecoveryOutbox(
                        UUID.randomUUID().toString().replace("-", ""),
                        "local-notification-reconcile:" + channelTaskId,
                        channelTaskId,
                        payload,
                        now
                );
            } catch (Exception e) {
                status.setRollbackOnly();
                throw new IllegalStateException("Failed to serialize reconciliation event", e);
            }
        });
    }
}
