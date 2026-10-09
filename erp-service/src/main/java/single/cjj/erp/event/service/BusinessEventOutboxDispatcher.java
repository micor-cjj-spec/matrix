package single.cjj.erp.event.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import single.cjj.erp.event.config.BusinessEventRabbitConfiguration;
import single.cjj.erp.event.entity.BusinessEventOutboxEntity;
import single.cjj.erp.event.mapper.BusinessEventOutboxMapper;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(name = "erp.business-event.publisher.enabled", havingValue = "true", matchIfMissing = true)
public class BusinessEventOutboxDispatcher {

    private final BusinessEventOutboxMapper mapper;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final int batchSize;
    private final int staleMinutes;
    private final Set<String> requiredRoutingKeys;

    public BusinessEventOutboxDispatcher(
            BusinessEventOutboxMapper mapper,
            RabbitTemplate rabbitTemplate,
            ObjectMapper objectMapper,
            @Value("${erp.business-event.publisher.batch-size:100}") int batchSize,
            @Value("${erp.business-event.publisher.stale-claim-minutes:10}") int staleMinutes,
            @Value("${erp.business-event.publisher.required-routing-keys:biz.procurement.purchase_inbound.confirmed}") String requiredRoutingKeys
    ) {
        this.mapper = mapper;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.batchSize = Math.max(1, batchSize);
        this.staleMinutes = Math.max(1, staleMinutes);
        this.requiredRoutingKeys = Arrays.stream(requiredRoutingKeys.split(","))
                .map(String::trim).filter(value -> !value.isEmpty()).collect(Collectors.toUnmodifiableSet());
    }

    @Scheduled(fixedDelayString = "${erp.business-event.publisher.fixed-delay-ms:3000}")
    public void publishDueEvents() {
        LocalDateTime now = LocalDateTime.now();
        mapper.recoverStaleClaims(now.minusMinutes(staleMinutes), now);
        List<BusinessEventOutboxEntity> events = mapper.findDue(now, batchSize);
        for (BusinessEventOutboxEntity event : events) {
            publishOne(event);
        }
    }

    private void publishOne(BusinessEventOutboxEntity event) {
        LocalDateTime now = LocalDateTime.now();
        String claimToken = UUID.randomUUID().toString().replace("-", "");
        if (mapper.claim(event.getFid(), claimToken, now) != 1) {
            return;
        }

        try {
            String envelope = buildEnvelope(event);
            CorrelationData correlationData = new CorrelationData(event.getFeventId());
            rabbitTemplate.convertAndSend(
                    BusinessEventRabbitConfiguration.BUSINESS_EVENT_EXCHANGE,
                    event.getFroutingKey(),
                    envelope,
                    message -> {
                        message.getMessageProperties().setMessageId(event.getFeventId());
                        message.getMessageProperties().setContentType("application/json");
                        message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                        message.getMessageProperties().setHeader("eventType", event.getFeventType());
                        message.getMessageProperties().setHeader("eventVersion", event.getFeventVersion());
                        message.getMessageProperties().setHeader("tenantId", event.getFtenantId());
                        return message;
                    },
                    correlationData
            );

            CorrelationData.Confirm confirm = correlationData.getFuture().get(5, TimeUnit.SECONDS);
            if (!confirm.isAck()) {
                throw new IllegalStateException("RabbitMQ publisher confirm rejected: " + confirm.getReason());
            }
            if (correlationData.getReturned() != null && requiredRoutingKeys.contains(event.getFroutingKey())) {
                throw new IllegalStateException("Required business event was unroutable: "
                        + event.getFroutingKey() + ", " + correlationData.getReturned().getReplyText());
            }
            mapper.markPublished(event.getFid(), claimToken, LocalDateTime.now());
        } catch (Exception exception) {
            int retryCount = nz(event.getFretryCount()) + 1;
            int maxRetry = Math.max(1, nz(event.getFmaxRetry()));
            boolean dead = retryCount >= maxRetry;
            LocalDateTime failureTime = LocalDateTime.now();
            LocalDateTime nextRetry = dead ? null : failureTime.plusSeconds(backoffSeconds(retryCount));
            mapper.markFailure(
                    event.getFid(),
                    claimToken,
                    dead ? "DEAD" : "FAILED",
                    retryCount,
                    nextRetry,
                    safeMessage(exception),
                    failureTime
            );
        }
    }

    private String buildEnvelope(BusinessEventOutboxEntity event) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("eventId", event.getFeventId());
        root.put("eventType", event.getFeventType());
        root.put("eventVersion", event.getFeventVersion());
        root.put("tenantId", event.getFtenantId());
        putNullable(root, "orgId", event.getForgId());
        root.put("producerService", event.getFproducerService());
        root.put("domainCode", event.getFdomainCode());
        root.put("aggregateType", event.getFaggregateType());
        root.put("aggregateId", event.getFaggregateId());
        putNullable(root, "aggregateVersion", event.getFaggregateVersion());
        root.put("sourceSystemCode", event.getFsourceSystemCode());
        root.put("sourceDocumentType", event.getFsourceDocumentType());
        root.put("sourceDocumentId", event.getFsourceDocumentId());
        root.put("sourceDocumentNo", event.getFsourceDocumentNo());
        if (event.getFbusinessDate() != null) {
            root.put("businessDate", event.getFbusinessDate().toString());
        }
        putNullable(root, "correlationId", event.getFcorrelationId());
        putNullable(root, "causationId", event.getFcausationId());
        putNullable(root, "traceId", event.getFtraceId());
        putNullable(root, "operatorId", event.getFoperatorId());
        JsonNode payload = event.getFpayloadJson() == null || event.getFpayloadJson().isBlank()
                ? objectMapper.createObjectNode()
                : objectMapper.readTree(event.getFpayloadJson());
        root.set("payload", payload);
        return objectMapper.writeValueAsString(root);
    }

    private void putNullable(ObjectNode root, String field, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof Number number) {
            root.put(field, number.longValue());
        } else {
            root.put(field, String.valueOf(value));
        }
    }

    private long backoffSeconds(int retryCount) {
        long seconds = 5L * (1L << Math.min(retryCount - 1, 7));
        return Math.min(600L, seconds);
    }

    private int nz(Integer value) {
        return value == null ? 0 : value;
    }

    private String safeMessage(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            message = throwable.getClass().getSimpleName();
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }
}
