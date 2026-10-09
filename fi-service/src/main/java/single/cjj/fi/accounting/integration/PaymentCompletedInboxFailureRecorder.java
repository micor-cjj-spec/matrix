package single.cjj.fi.accounting.integration;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import single.cjj.fi.accounting.persistence.InboundAccountingRepository;

import java.util.UUID;

@Service
public class PaymentCompletedInboxFailureRecorder {

    private final ObjectMapper objectMapper;
    private final InboundAccountingRepository repository;
    private final String consumerCode;

    public PaymentCompletedInboxFailureRecorder(
            ObjectMapper objectMapper,
            InboundAccountingRepository repository,
            @Value("${fi.accounting.payment-completed-consumer-code:FI_PAYMENT_COMPLETED_ACCOUNTING_V1}")
            String consumerCode
    ) {
        this.objectMapper = objectMapper;
        this.repository = repository;
        this.consumerCode = consumerCode;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String rawJson, String messageId, Throwable throwable) {
        String eventId = messageId;
        String tenantId = "UNKNOWN";
        Long orgId = null;
        String eventType = "UNKNOWN";
        String producerService = "fi-service";
        String safeJson = "{}";
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            safeJson = objectMapper.writeValueAsString(root);
            if ((eventId == null || eventId.isBlank()) && root.hasNonNull("eventId")) {
                eventId = root.path("eventId").asText();
            }
            if (root.hasNonNull("tenantId")) {
                tenantId = root.path("tenantId").asText();
            }
            if (root.hasNonNull("orgId")) {
                orgId = root.path("orgId").asLong();
            }
            if (root.hasNonNull("eventType")) {
                eventType = root.path("eventType").asText();
            }
            if (root.hasNonNull("producerService")) {
                producerService = root.path("producerService").asText();
            }
        } catch (Exception ignored) {
            // keep a failure record even when payload itself is malformed.
        }
        if (eventId == null || eventId.isBlank()) {
            eventId = "FAILED-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
        }
        String error = throwable == null ? "UNKNOWN_ERROR" : throwable.getMessage();
        if (error == null || error.isBlank()) {
            error = throwable == null ? "UNKNOWN_ERROR" : throwable.getClass().getSimpleName();
        }
        repository.recordInboxFailure(
                IdWorker.getId(),
                consumerCode,
                eventId,
                tenantId,
                orgId,
                eventType,
                producerService,
                safeJson,
                error
        );
    }
}
