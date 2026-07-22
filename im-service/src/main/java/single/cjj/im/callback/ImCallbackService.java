package single.cjj.im.callback;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import single.cjj.im.application.ImApplicationService;
import single.cjj.im.application.ImApplicationService.AuthenticatedApplication;
import single.cjj.im.application.ImSecretCodec;
import single.cjj.im.domain.ImModels.ChannelStatusResponse;
import single.cjj.im.repository.ImMessageRepository;
import single.cjj.im.reliability.ImOperationsRepository;
import single.cjj.im.reliability.ImOperationsRepository.CallbackTaskRecord;
import single.cjj.im.reliability.ImOperationsRepository.FinalMessageCandidate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.client.RestClient;

@Service
public class ImCallbackService {

    private final ImOperationsRepository operationsRepository;
    private final ImMessageRepository messageRepository;
    private final ImApplicationService applicationService;
    private final ImSecretCodec secretCodec;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final int batchSize;
    private final int maxRetryCount;
    private final int processingTimeoutMinutes;

    public ImCallbackService(ImOperationsRepository operationsRepository,
                             ImMessageRepository messageRepository,
                             ImApplicationService applicationService,
                             ImSecretCodec secretCodec,
                             ObjectMapper objectMapper,
                             RestClient.Builder restClientBuilder,
                             @Value("${im.callback.batch-size:100}") int batchSize,
                             @Value("${im.callback.max-retry-count:6}") int maxRetryCount,
                             @Value("${im.callback.processing-timeout-minutes:10}") int processingTimeoutMinutes) {
        this.operationsRepository = operationsRepository;
        this.messageRepository = messageRepository;
        this.applicationService = applicationService;
        this.secretCodec = secretCodec;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder.build();
        this.batchSize = Math.max(1, Math.min(batchSize, 500));
        this.maxRetryCount = Math.max(1, maxRetryCount);
        this.processingTimeoutMinutes = Math.max(1, processingTimeoutMinutes);
    }

    @Scheduled(fixedDelayString = "${im.callback.prepare-delay-ms:3000}")
    public void prepareFinalCallbacks() {
        List<FinalMessageCandidate> candidates =
                operationsRepository.findFinalMessagesMissingCallbacks(batchSize);
        for (FinalMessageCandidate candidate : candidates) {
            try {
                prepareOne(candidate);
            } catch (Exception e) {
                operationsRepository.updateMessageCallbackStatus(
                        candidate.messageId(),
                        "CONFIG_ERROR",
                        LocalDateTime.now()
                );
            }
        }
    }

    @Scheduled(fixedDelayString = "${im.callback.dispatch-delay-ms:3000}")
    public void dispatchDueCallbacks() {
        LocalDateTime now = LocalDateTime.now();
        for (CallbackTaskRecord task : operationsRepository.findDueCallbackTasks(now, batchSize)) {
            if (operationsRepository.claimCallback(task.id(), now) == 0) {
                continue;
            }
            dispatchOne(task);
        }
    }

    @Scheduled(fixedDelayString = "${im.callback.recovery-delay-ms:60000}")
    public void recoverStaleCallbacks() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = now.minusMinutes(processingTimeoutMinutes);
        for (String id : operationsRepository.findStaleCallbackIds(deadline, batchSize)) {
            operationsRepository.recoverStaleCallback(id, now.plusMinutes(1), now);
        }
    }

    private void prepareOne(FinalMessageCandidate candidate) {
        AuthenticatedApplication application = applicationService.requireEnabled(candidate.appCode());
        if (!candidate.callbackUrl().equals(application.callbackUrl())) {
            throw new IllegalStateException("Message callback URL no longer matches registered application callback");
        }
        String eventId = "message-final:" + candidate.messageNo() + ":" + candidate.messageStatus();
        String payload = payload(candidate, eventId);
        LocalDateTime now = LocalDateTime.now();
        int inserted = operationsRepository.insertCallbackTask(new CallbackTaskRecord(
                uuid(),
                eventId,
                candidate.messageId(),
                candidate.messageStatus(),
                candidate.appCode(),
                candidate.callbackUrl(),
                secretCodec.encrypt(application.callbackSecret()),
                payload,
                "PENDING",
                0,
                maxRetryCount,
                now,
                null,
                null,
                null,
                null,
                now,
                now
        ));
        if (inserted > 0) {
            operationsRepository.updateMessageCallbackStatus(candidate.messageId(), "PENDING", now);
        }
    }

    private void dispatchOne(CallbackTaskRecord task) {
        int responseCode = 0;
        String responseBody = null;
        try {
            String timestamp = String.valueOf(System.currentTimeMillis());
            String nonce = UUID.randomUUID().toString().replace("-", "");
            String signature = sign(
                    secretCodec.decrypt(task.callbackSecretCiphertext()),
                    task.callbackUrl(),
                    timestamp,
                    nonce,
                    task.payloadJson()
            );
            ResponseEntity<String> response = restClient.post()
                    .uri(task.callbackUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-IM-Timestamp", timestamp)
                    .header("X-IM-Nonce", nonce)
                    .header("X-IM-Signature", signature)
                    .header("X-IM-Event-Id", task.eventId())
                    .body(task.payloadJson())
                    .retrieve()
                    .toEntity(String.class);
            responseCode = response.getStatusCode().value();
            responseBody = response.getBody();
            operationsRepository.markCallbackSuccess(task.id(), responseCode, responseBody, LocalDateTime.now());
            operationsRepository.updateMessageCallbackStatus(task.messageId(), "SUCCESS", LocalDateTime.now());
        } catch (Exception e) {
            int retryCount = task.retryCount() + 1;
            String error = safeMessage(e);
            if (retryCount >= task.maxRetryCount()) {
                operationsRepository.markCallbackDead(
                        task.id(), retryCount, responseCode == 0 ? null : responseCode,
                        responseBody, error, LocalDateTime.now());
                operationsRepository.updateMessageCallbackStatus(task.messageId(), "DEAD", LocalDateTime.now());
            } else {
                operationsRepository.markCallbackRetry(
                        task.id(), retryCount, nextRetryTime(retryCount),
                        responseCode == 0 ? null : responseCode, responseBody, error, LocalDateTime.now());
                operationsRepository.updateMessageCallbackStatus(task.messageId(), "RETRYING", LocalDateTime.now());
            }
        }
    }

    private String payload(FinalMessageCandidate candidate, String eventId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("eventId", eventId);
        body.put("messageNo", candidate.messageNo());
        body.put("requestId", candidate.requestId());
        body.put("status", candidate.messageStatus());
        body.put("occurredTime", candidate.occurredTime());
        List<Map<String, Object>> channels = new ArrayList<>();
        for (ChannelStatusResponse channel : messageRepository.findChannelStatusResponses(candidate.messageId())) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("channelTaskId", channel.channelTaskId());
            item.put("recipientId", channel.recipientId());
            item.put("channel", channel.channel());
            item.put("status", channel.status());
            item.put("retryCount", channel.retryCount());
            item.put("providerMessageId", channel.providerMessageId());
            item.put("errorCode", channel.errorCode());
            item.put("errorMessage", channel.errorMessage());
            channels.add(item);
        }
        body.put("channels", channels);
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Callback payload serialization failed", e);
        }
    }

    public static String sign(String secret,
                              String callbackUrl,
                              String timestamp,
                              String nonce,
                              String payload) {
        try {
            String path = URI.create(callbackUrl).getRawPath();
            String bodyHash = HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(payload.getBytes(StandardCharsets.UTF_8)));
            String canonical = "POST\n" + path + "\n" + timestamp + "\n" + nonce + "\n" + bodyHash;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Callback signature generation failed", e);
        }
    }

    private LocalDateTime nextRetryTime(int retryCount) {
        long minutes = switch (retryCount) {
            case 1 -> 1;
            case 2 -> 5;
            case 3 -> 15;
            case 4 -> 60;
            case 5 -> 360;
            default -> 1440;
        };
        return LocalDateTime.now().plusMinutes(minutes);
    }

    private String safeMessage(Exception e) {
        return e.getMessage() == null || e.getMessage().isBlank()
                ? e.getClass().getSimpleName()
                : e.getMessage();
    }

    private String uuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
