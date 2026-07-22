package single.cjj.im.worker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.support.TransactionTemplate;
import single.cjj.im.config.ImRabbitConfiguration;
import single.cjj.im.domain.ImModels.ChannelTaskRecord;
import single.cjj.im.domain.ImModels.ChannelType;
import single.cjj.im.domain.ImModels.OutboxRecord;
import single.cjj.im.domain.ImModels.OutboxStatus;
import single.cjj.im.repository.ImMessageRepository;
import single.cjj.im.service.ChannelDispatchService;
import single.cjj.im.service.ImBusinessException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Configuration
public class ImWorkerConfiguration {

    @Bean
    OutboxDispatcher outboxDispatcher(ImMessageRepository repository,
                                      RabbitTemplate rabbitTemplate,
                                      @Value("${im.outbox.batch-size:100}") int batchSize) {
        return new OutboxDispatcher(repository, rabbitTemplate, batchSize);
    }

    @Bean
    ChannelTaskConsumer channelTaskConsumer(ObjectMapper objectMapper,
                                            ChannelDispatchService dispatchService) {
        return new ChannelTaskConsumer(objectMapper, dispatchService);
    }

    @Bean
    StaleTaskRecoveryJob staleTaskRecoveryJob(ImMessageRepository repository,
                                              ChannelDispatchService dispatchService,
                                              ObjectMapper objectMapper,
                                              TransactionTemplate transactionTemplate,
                                              @Value("${im.channel.processing-timeout-minutes:10}") int timeoutMinutes) {
        return new StaleTaskRecoveryJob(
                repository,
                dispatchService,
                objectMapper,
                transactionTemplate,
                Math.max(1, timeoutMinutes)
        );
    }
}

class OutboxDispatcher {

    private static final int MAX_RETRY_COUNT = 10;

    private final ImMessageRepository repository;
    private final RabbitTemplate rabbitTemplate;
    private final int batchSize;

    OutboxDispatcher(ImMessageRepository repository, RabbitTemplate rabbitTemplate, int batchSize) {
        this.repository = repository;
        this.rabbitTemplate = rabbitTemplate;
        this.batchSize = Math.max(1, batchSize);
    }

    @Scheduled(fixedDelayString = "${im.outbox.fixed-delay-ms:3000}")
    public void publishDueEvents() {
        LocalDateTime now = LocalDateTime.now();
        List<OutboxRecord> events = repository.findDueOutbox(now, batchSize);
        for (OutboxRecord event : events) {
            if (repository.claimOutbox(event.id()) == 0) {
                continue;
            }
            try {
                CorrelationData correlationData = new CorrelationData(event.eventId());
                rabbitTemplate.convertAndSend(
                        ImRabbitConfiguration.EXCHANGE,
                        ImRabbitConfiguration.CHANNEL_TASK_ROUTING_KEY,
                        event.payloadJson(),
                        correlationData
                );
                CorrelationData.Confirm confirm = correlationData.getFuture().get(5, TimeUnit.SECONDS);
                if (!confirm.isAck()) {
                    throw new IllegalStateException("RabbitMQ publisher confirm rejected: " + confirm.getReason());
                }
                repository.markOutboxPublished(event.id(), LocalDateTime.now());
            } catch (Exception e) {
                int retryCount = event.retryCount() + 1;
                if (retryCount >= MAX_RETRY_COUNT) {
                    repository.markOutboxDead(event.id(), retryCount, safeMessage(e));
                } else {
                    repository.markOutboxRetry(
                            event.id(),
                            retryCount,
                            LocalDateTime.now().plusMinutes(Math.min(60, retryCount * 2L)),
                            safeMessage(e)
                    );
                }
            }
        }
    }

    private String safeMessage(Exception e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }
}

class ChannelTaskConsumer {

    private final ObjectMapper objectMapper;
    private final ChannelDispatchService dispatchService;

    ChannelTaskConsumer(ObjectMapper objectMapper, ChannelDispatchService dispatchService) {
        this.objectMapper = objectMapper;
        this.dispatchService = dispatchService;
    }

    @RabbitListener(queues = ImRabbitConfiguration.CHANNEL_TASK_QUEUE)
    public void consume(String payload, Channel channel, Message amqpMessage) throws IOException {
        long deliveryTag = amqpMessage.getMessageProperties().getDeliveryTag();
        try {
            JsonNode root = objectMapper.readTree(payload);
            String channelTaskId = root.path("channelTaskId").asText();
            if (channelTaskId.isBlank()) {
                channel.basicReject(deliveryTag, false);
                return;
            }
            dispatchService.dispatch(channelTaskId);
            channel.basicAck(deliveryTag, false);
        } catch (ImBusinessException e) {
            channel.basicReject(deliveryTag, false);
        } catch (Exception e) {
            channel.basicNack(deliveryTag, false, true);
        }
    }
}

class StaleTaskRecoveryJob {

    private final ImMessageRepository repository;
    private final ChannelDispatchService dispatchService;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private final int timeoutMinutes;

    StaleTaskRecoveryJob(ImMessageRepository repository,
                         ChannelDispatchService dispatchService,
                         ObjectMapper objectMapper,
                         TransactionTemplate transactionTemplate,
                         int timeoutMinutes) {
        this.repository = repository;
        this.dispatchService = dispatchService;
        this.objectMapper = objectMapper;
        this.transactionTemplate = transactionTemplate;
        this.timeoutMinutes = timeoutMinutes;
    }

    @Scheduled(fixedDelay = 60000L)
    public void recover() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = now.minusMinutes(timeoutMinutes);
        recoverLocalTasks(repository.findStaleChannelTasks(ChannelType.LOCAL, deadline, 100), now);
        markUnknownEmailTasks(repository.findStaleChannelTasks(ChannelType.EMAIL, deadline, 100), now);
    }

    private void recoverLocalTasks(List<ChannelTaskRecord> tasks, LocalDateTime now) {
        for (ChannelTaskRecord task : tasks) {
            int nextRetryCount = task.retryCount() + 1;
            if (nextRetryCount >= task.maxRetryCount()) {
                repository.markChannelDead(task.id(), nextRetryCount,
                        "PROCESSING_TIMEOUT", "本地提醒执行多次超时，已停止自动重试", now);
                dispatchService.refreshMessageStatus(task.messageId());
                continue;
            }
            transactionTemplate.executeWithoutResult(status -> {
                if (repository.recoverStaleLocalTask(task.id(), nextRetryCount, now, now) == 0) {
                    return;
                }
                repository.insertOutbox(new OutboxRecord(
                        UUID.randomUUID().toString().replace("-", ""),
                        "stale-local-retry:" + task.id() + ":" + now,
                        "CHANNEL_TASK",
                        task.id(),
                        "CHANNEL_TASK_RECOVERED",
                        payload(task.id()),
                        OutboxStatus.PENDING,
                        0,
                        now,
                        null,
                        now,
                        null
                ));
            });
        }
    }

    private void markUnknownEmailTasks(List<ChannelTaskRecord> tasks, LocalDateTime now) {
        for (ChannelTaskRecord task : tasks) {
            if (repository.markStaleEmailTaskUnknown(task.id(), now) > 0) {
                dispatchService.refreshMessageStatus(task.messageId());
            }
        }
    }

    private String payload(String channelTaskId) {
        try {
            return objectMapper.writeValueAsString(Map.of("channelTaskId", channelTaskId));
        } catch (Exception e) {
            throw new ImBusinessException("恢复事件序列化失败", e);
        }
    }
}
