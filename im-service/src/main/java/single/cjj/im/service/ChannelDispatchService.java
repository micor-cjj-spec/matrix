package single.cjj.im.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import single.cjj.im.config.ChannelDeliveryConfiguration.ChannelHandler;
import single.cjj.im.domain.ImModels.ChannelSendResult;
import single.cjj.im.domain.ImModels.ChannelStatus;
import single.cjj.im.domain.ImModels.ChannelTaskRecord;
import single.cjj.im.domain.ImModels.MessageRecord;
import single.cjj.im.domain.ImModels.MessageStatus;
import single.cjj.im.domain.ImModels.OutboxRecord;
import single.cjj.im.domain.ImModels.OutboxStatus;
import single.cjj.im.domain.ImModels.RecipientRecord;
import single.cjj.im.repository.ImMessageRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ChannelDispatchService {

    private static final Set<String> FINAL_CHANNEL_STATUSES = Set.of(
            ChannelStatus.SUCCESS,
            ChannelStatus.DEAD,
            ChannelStatus.UNKNOWN,
            ChannelStatus.CANCELLED,
            ChannelStatus.EXPIRED
    );

    private final ImMessageRepository repository;
    private final Map<String, ChannelHandler> handlers;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    public ChannelDispatchService(ImMessageRepository repository,
                                  List<ChannelHandler> handlers,
                                  ObjectMapper objectMapper,
                                  TransactionTemplate transactionTemplate) {
        this.repository = repository;
        Map<String, ChannelHandler> handlerMap = new HashMap<>();
        for (ChannelHandler handler : handlers) {
            if (handler.supports("LOCAL")) {
                handlerMap.put("LOCAL", handler);
            }
            if (handler.supports("EMAIL")) {
                handlerMap.put("EMAIL", handler);
            }
        }
        this.handlers = Map.copyOf(handlerMap);
        this.objectMapper = objectMapper;
        this.transactionTemplate = transactionTemplate;
    }

    public void dispatch(String channelTaskId) {
        ChannelTaskRecord initialTask = repository.findChannelTask(channelTaskId)
                .orElseThrow(() -> new ImBusinessException("渠道任务不存在: " + channelTaskId));
        if (FINAL_CHANNEL_STATUSES.contains(initialTask.status())) {
            return;
        }

        MessageRecord message = repository.findMessageById(initialTask.messageId())
                .orElseThrow(() -> new ImBusinessException("消息主任务不存在"));
        LocalDateTime now = LocalDateTime.now();
        if (message.expireTime() != null && !message.expireTime().isAfter(now)) {
            repository.markChannelExpired(channelTaskId, now);
            refreshMessageStatus(message.id());
            return;
        }

        if (repository.claimChannelTask(channelTaskId, now) == 0) {
            return;
        }

        ChannelTaskRecord claimedTask = repository.findChannelTask(channelTaskId).orElse(initialTask);
        RecipientRecord recipient = repository.findRecipient(claimedTask.recipientId())
                .orElseThrow(() -> new ImBusinessException("消息接收人不存在"));
        ChannelHandler handler = handlers.get(claimedTask.channelType());
        if (handler == null) {
            repository.markChannelDead(channelTaskId, claimedTask.retryCount(),
                    "CHANNEL_NOT_SUPPORTED", "未注册渠道处理器: " + claimedTask.channelType(), now);
            refreshMessageStatus(message.id());
            return;
        }

        ChannelSendResult result;
        try {
            result = handler.send(message, recipient, claimedTask);
        } catch (Exception e) {
            result = ChannelSendResult.retryable("CHANNEL_HANDLER_ERROR", safeMessage(e));
        }

        if (result.success()) {
            repository.markChannelSuccess(channelTaskId, result.providerMessageId(), LocalDateTime.now());
            refreshMessageStatus(message.id());
            return;
        }

        int nextRetryCount = claimedTask.retryCount() + 1;
        if (result.retryable() && nextRetryCount < claimedTask.maxRetryCount()) {
            LocalDateTime nextRetryTime = RetryBackoffPolicy.nextRetryTime(nextRetryCount, LocalDateTime.now());
            transactionTemplate.executeWithoutResult(status -> {
                repository.markChannelRetry(
                        channelTaskId,
                        nextRetryCount,
                        nextRetryTime,
                        result.errorCode(),
                        result.errorMessage(),
                        LocalDateTime.now()
                );
                repository.insertOutbox(retryOutbox(channelTaskId, nextRetryCount, nextRetryTime));
            });
        } else {
            repository.markChannelDead(
                    channelTaskId,
                    nextRetryCount,
                    result.errorCode(),
                    result.errorMessage(),
                    LocalDateTime.now()
            );
        }
        refreshMessageStatus(message.id());
    }

    public void refreshMessageStatus(String messageId) {
        List<String> statuses = repository.findChannelStatuses(messageId);
        if (statuses.isEmpty()) {
            return;
        }
        int success = count(statuses, ChannelStatus.SUCCESS);
        int unknown = count(statuses, ChannelStatus.UNKNOWN);
        int failed = countAny(statuses, Set.of(
                ChannelStatus.FAILED,
                ChannelStatus.DEAD,
                ChannelStatus.EXPIRED,
                ChannelStatus.CANCELLED
        ));
        int active = countAny(statuses, Set.of(
                ChannelStatus.PENDING,
                ChannelStatus.PROCESSING,
                ChannelStatus.RETRYING
        ));

        String aggregateStatus;
        if (active > 0) {
            aggregateStatus = MessageStatus.PROCESSING;
        } else if (unknown > 0) {
            aggregateStatus = MessageStatus.UNKNOWN;
        } else if (success == statuses.size()) {
            aggregateStatus = MessageStatus.SUCCESS;
        } else if (success > 0) {
            aggregateStatus = MessageStatus.PARTIAL_SUCCESS;
        } else {
            aggregateStatus = MessageStatus.FAILED;
        }
        repository.updateMessageAggregate(
                messageId,
                aggregateStatus,
                success,
                failed + unknown,
                LocalDateTime.now()
        );
    }

    private OutboxRecord retryOutbox(String channelTaskId, int retryCount, LocalDateTime nextRetryTime) {
        return new OutboxRecord(
                UUID.randomUUID().toString().replace("-", ""),
                "channel-task-retry:" + channelTaskId + ":" + retryCount,
                "CHANNEL_TASK",
                channelTaskId,
                "CHANNEL_TASK_RETRY",
                payload(channelTaskId),
                OutboxStatus.PENDING,
                0,
                nextRetryTime,
                null,
                LocalDateTime.now(),
                null
        );
    }

    private String payload(String channelTaskId) {
        try {
            return objectMapper.writeValueAsString(Map.of("channelTaskId", channelTaskId));
        } catch (JsonProcessingException e) {
            throw new ImBusinessException("重试事件序列化失败", e);
        }
    }

    private int count(List<String> statuses, String target) {
        return (int) statuses.stream().filter(target::equals).count();
    }

    private int countAny(List<String> statuses, Set<String> targets) {
        return (int) statuses.stream().filter(targets::contains).count();
    }

    private String safeMessage(Exception e) {
        String message = e.getMessage();
        return message == null || message.isBlank() ? e.getClass().getSimpleName() : message;
    }

    static final class RetryBackoffPolicy {
        private RetryBackoffPolicy() {
        }

        static LocalDateTime nextRetryTime(int retryCount, LocalDateTime baseTime) {
            long minutes = switch (retryCount) {
                case 1 -> 1;
                case 2 -> 5;
                case 3 -> 15;
                case 4 -> 60;
                default -> 360;
            };
            return baseTime.plusMinutes(minutes);
        }
    }
}
