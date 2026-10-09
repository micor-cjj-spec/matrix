package single.cjj.openapi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import single.cjj.openapi.entity.OpenApiOutboxEvent;
import single.cjj.openapi.mapper.OpenApiOutboxEventMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class OpenApiOutboxDispatcher {

    private static final Set<String> DISPATCHABLE = Set.of("PENDING", "FAILED");

    private final OpenApiOutboxEventMapper outboxMapper;
    private final RabbitTemplate rabbitTemplate;
    private final OpenApiWriteStateService stateService;
    private final String exchangeName;
    private final String routingKey;
    private final long confirmTimeoutMs;

    public OpenApiOutboxDispatcher(OpenApiOutboxEventMapper outboxMapper,
                                   RabbitTemplate rabbitTemplate,
                                   OpenApiWriteStateService stateService,
                                   @Value("${matrix.openapi.write.exchange:matrix.openapi.write.exchange}") String exchangeName,
                                   @Value("${matrix.openapi.write.routing-key:matrix.openapi.voucher.write}") String routingKey,
                                   @Value("${matrix.openapi.write.publisher-confirm-timeout-ms:5000}") long confirmTimeoutMs) {
        this.outboxMapper = outboxMapper;
        this.rabbitTemplate = rabbitTemplate;
        this.stateService = stateService;
        this.exchangeName = exchangeName;
        this.routingKey = routingKey;
        this.confirmTimeoutMs = Math.max(1000L, confirmTimeoutMs);
    }

    @Scheduled(fixedDelayString = "${matrix.openapi.write.outbox-poll-ms:2000}")
    public void dispatch() {
        LocalDateTime now = LocalDateTime.now();
        List<OpenApiOutboxEvent> events = outboxMapper.selectList(
                new LambdaQueryWrapper<OpenApiOutboxEvent>()
                        .in(OpenApiOutboxEvent::getStatus, DISPATCHABLE)
                        .le(OpenApiOutboxEvent::getNextAttemptAt, now)
                        .orderByAsc(OpenApiOutboxEvent::getId)
                        .last("LIMIT 50")
        );
        for (OpenApiOutboxEvent event : events) {
            dispatchOne(event);
        }
    }

    @Scheduled(fixedDelayString = "${matrix.openapi.write.recovery-poll-ms:60000}")
    public void recoverStaleProcessing() {
        int recovered = stateService.recoverStaleProcessing();
        if (recovered > 0) {
            log.warn("recovered {} stale OpenAPI voucher write requests", recovered);
        }
    }

    private void dispatchOne(OpenApiOutboxEvent event) {
        LocalDateTime now = LocalDateTime.now();
        int claimed = outboxMapper.update(null, new LambdaUpdateWrapper<OpenApiOutboxEvent>()
                .eq(OpenApiOutboxEvent::getId, event.getId())
                .in(OpenApiOutboxEvent::getStatus, DISPATCHABLE)
                .set(OpenApiOutboxEvent::getStatus, "SENDING")
                .set(OpenApiOutboxEvent::getUpdatedAt, now));
        if (claimed == 0) {
            return;
        }

        try {
            CorrelationData correlationData = new CorrelationData(event.getEventId());
            rabbitTemplate.convertAndSend(
                    exchangeName,
                    routingKey,
                    String.valueOf(event.getAggregateId()),
                    correlationData
            );
            CorrelationData.Confirm confirm = correlationData.getFuture()
                    .get(confirmTimeoutMs, TimeUnit.MILLISECONDS);
            if (!confirm.isAck()) {
                throw new IllegalStateException(
                        "RabbitMQ broker rejected message: " + confirm.getReason()
                );
            }
            outboxMapper.update(null, new LambdaUpdateWrapper<OpenApiOutboxEvent>()
                    .eq(OpenApiOutboxEvent::getId, event.getId())
                    .eq(OpenApiOutboxEvent::getStatus, "SENDING")
                    .set(OpenApiOutboxEvent::getStatus, "SENT")
                    .set(OpenApiOutboxEvent::getSentAt, LocalDateTime.now())
                    .set(OpenApiOutboxEvent::getErrorMessage, null)
                    .set(OpenApiOutboxEvent::getUpdatedAt, LocalDateTime.now()));
        } catch (Exception e) {
            int retryCount = (event.getRetryCount() == null ? 0 : event.getRetryCount()) + 1;
            int maxRetry = event.getMaxRetry() == null ? 10 : event.getMaxRetry();
            boolean exhausted = retryCount >= maxRetry;
            outboxMapper.update(null, new LambdaUpdateWrapper<OpenApiOutboxEvent>()
                    .eq(OpenApiOutboxEvent::getId, event.getId())
                    .eq(OpenApiOutboxEvent::getStatus, "SENDING")
                    .set(OpenApiOutboxEvent::getStatus, exhausted ? "DEAD" : "FAILED")
                    .set(OpenApiOutboxEvent::getRetryCount, retryCount)
                    .set(OpenApiOutboxEvent::getNextAttemptAt,
                            exhausted ? null : LocalDateTime.now().plusSeconds(retryDelaySeconds(retryCount)))
                    .set(OpenApiOutboxEvent::getErrorMessage, truncate(e.getMessage(), 1000))
                    .set(OpenApiOutboxEvent::getUpdatedAt, LocalDateTime.now()));
            log.warn("dispatch OpenAPI outbox failed, eventId={}, retry={}, message={}",
                    event.getEventId(), retryCount, e.getMessage());
        }
    }

    private long retryDelaySeconds(int retryCount) {
        return Math.min(300L, 5L * (1L << Math.min(retryCount, 6)));
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
