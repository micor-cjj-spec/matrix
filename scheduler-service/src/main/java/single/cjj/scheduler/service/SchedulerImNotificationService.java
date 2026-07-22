package single.cjj.scheduler.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import single.cjj.scheduler.entity.MatrixSchedulerAlertRecord;
import single.cjj.scheduler.entity.MatrixSchedulerImOutbox;
import single.cjj.scheduler.mapper.MatrixSchedulerImOutboxMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SchedulerImNotificationService {

    private static final String SEND_PATH = "/open-api/v1/messages/send";

    private final MatrixSchedulerImOutboxMapper outboxMapper;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final boolean enabled;
    private final String imBaseUrl;
    private final String appCode;
    private final String appSecret;
    private final String tenantId;
    private final String callbackUrl;
    private final String callbackSecret;
    private final List<String> recipientUserIds;
    private final List<String> channels;
    private final int batchSize;
    private final int maxRetryCount;
    private final int processingTimeoutMinutes;

    public SchedulerImNotificationService(
            MatrixSchedulerImOutboxMapper outboxMapper,
            ObjectMapper objectMapper,
            RestClient.Builder restClientBuilder,
            @Value("${matrix.scheduler.im.enabled:false}") boolean enabled,
            @Value("${matrix.scheduler.im.base-url:http://127.0.0.1:10000/api}") String imBaseUrl,
            @Value("${matrix.scheduler.im.app-code:scheduler}") String appCode,
            @Value("${matrix.scheduler.im.app-secret:}") String appSecret,
            @Value("${matrix.scheduler.im.tenant-id:default}") String tenantId,
            @Value("${matrix.scheduler.im.callback-url:}") String callbackUrl,
            @Value("${matrix.scheduler.im.callback-secret:}") String callbackSecret,
            @Value("${matrix.scheduler.im.recipient-user-ids:}") String recipientUserIds,
            @Value("${matrix.scheduler.im.channels:LOCAL}") String channels,
            @Value("${matrix.scheduler.im.batch-size:100}") int batchSize,
            @Value("${matrix.scheduler.im.max-retry-count:8}") int maxRetryCount,
            @Value("${matrix.scheduler.im.processing-timeout-minutes:5}") int processingTimeoutMinutes) {
        this.outboxMapper = outboxMapper;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder.build();
        this.enabled = enabled;
        this.imBaseUrl = trimTrailingSlash(imBaseUrl);
        this.appCode = appCode;
        this.appSecret = appSecret;
        this.tenantId = tenantId;
        this.callbackUrl = callbackUrl;
        this.callbackSecret = callbackSecret;
        this.recipientUserIds = split(recipientUserIds);
        this.channels = split(channels);
        this.batchSize = Math.max(1, Math.min(batchSize, 500));
        this.maxRetryCount = Math.max(1, maxRetryCount);
        this.processingTimeoutMinutes = Math.max(1, processingTimeoutMinutes);
    }

    @Transactional(rollbackFor = Exception.class)
    public void enqueue(MatrixSchedulerAlertRecord alert) {
        if (!enabled) {
            return;
        }
        String requestId = "scheduler-alert:" + alert.getFid();
        Long existing = outboxMapper.selectCount(new LambdaQueryWrapper<MatrixSchedulerImOutbox>()
                .eq(MatrixSchedulerImOutbox::getFrequestId, requestId));
        if (existing != null && existing > 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        MatrixSchedulerImOutbox outbox = new MatrixSchedulerImOutbox();
        outbox.setFid(IdWorker.getId());
        outbox.setFalertId(alert.getFid());
        outbox.setFrequestId(requestId);
        outbox.setFpayload(buildPayload(alert, requestId));
        outbox.setFstatus("PENDING");
        outbox.setFretryCount(0);
        outbox.setFnextRetryTime(now);
        outbox.setFcallbackStatus("PENDING");
        outbox.setFcreateTime(now);
        outbox.setFupdateTime(now);
        outboxMapper.insert(outbox);
    }

    @Scheduled(fixedDelayString = "${matrix.scheduler.im.dispatch-delay-ms:5000}")
    public void dispatchPending() {
        if (!enabled) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        List<MatrixSchedulerImOutbox> tasks = outboxMapper.selectList(
                new LambdaQueryWrapper<MatrixSchedulerImOutbox>()
                        .in(MatrixSchedulerImOutbox::getFstatus, "PENDING", "RETRYING")
                        .and(wrapper -> wrapper.isNull(MatrixSchedulerImOutbox::getFnextRetryTime)
                                .or()
                                .le(MatrixSchedulerImOutbox::getFnextRetryTime, now))
                        .orderByAsc(MatrixSchedulerImOutbox::getFcreateTime)
                        .last("LIMIT " + batchSize));
        for (MatrixSchedulerImOutbox task : tasks) {
            if (!claim(task.getFid(), now)) {
                continue;
            }
            deliver(task);
        }
    }

    @Scheduled(fixedDelayString = "${matrix.scheduler.im.recovery-delay-ms:60000}")
    public void recoverStale() {
        if (!enabled) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = now.minusMinutes(processingTimeoutMinutes);
        List<MatrixSchedulerImOutbox> stale = outboxMapper.selectList(
                new LambdaQueryWrapper<MatrixSchedulerImOutbox>()
                        .eq(MatrixSchedulerImOutbox::getFstatus, "PROCESSING")
                        .lt(MatrixSchedulerImOutbox::getFprocessingStartedTime, deadline)
                        .orderByAsc(MatrixSchedulerImOutbox::getFprocessingStartedTime)
                        .last("LIMIT " + batchSize));
        for (MatrixSchedulerImOutbox task : stale) {
            int retryCount = task.getFretryCount() == null ? 1 : task.getFretryCount() + 1;
            task.setFretryCount(retryCount);
            task.setFstatus(retryCount >= maxRetryCount ? "DEAD" : "RETRYING");
            task.setFnextRetryTime(retryCount >= maxRetryCount ? null : now.plusMinutes(1));
            task.setFprocessingStartedTime(null);
            task.setFlastError("IM_DISPATCH_PROCESSING_TIMEOUT");
            task.setFupdateTime(now);
            outboxMapper.updateById(task);
        }
    }

    public void acceptCallback(String timestamp,
                               String nonce,
                               String signature,
                               String eventId,
                               String rawBody) {
        validateCallbackSignature(timestamp, nonce, signature, rawBody);
        try {
            JsonNode root = objectMapper.readTree(rawBody);
            String requestId = root.path("requestId").asText();
            String status = root.path("status").asText();
            if (!StringUtils.hasText(requestId) || !StringUtils.hasText(status)) {
                throw new IllegalArgumentException("IM callback missing requestId or status");
            }
            MatrixSchedulerImOutbox task = outboxMapper.selectOne(
                    new LambdaQueryWrapper<MatrixSchedulerImOutbox>()
                            .eq(MatrixSchedulerImOutbox::getFrequestId, requestId)
                            .last("LIMIT 1"));
            if (task == null) {
                throw new IllegalArgumentException("Scheduler IM outbox not found");
            }
            if (eventId.equals(task.getFcallbackEventId())) {
                return;
            }
            task.setFcallbackEventId(eventId);
            task.setFcallbackStatus(status);
            task.setFstatus("CALLBACK_SUCCESS");
            task.setFupdateTime(LocalDateTime.now());
            outboxMapper.updateById(task);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid IM callback payload", e);
        }
    }

    private boolean claim(Long id, LocalDateTime now) {
        return outboxMapper.update(null, new LambdaUpdateWrapper<MatrixSchedulerImOutbox>()
                .set(MatrixSchedulerImOutbox::getFstatus, "PROCESSING")
                .set(MatrixSchedulerImOutbox::getFprocessingStartedTime, now)
                .set(MatrixSchedulerImOutbox::getFupdateTime, now)
                .eq(MatrixSchedulerImOutbox::getFid, id)
                .in(MatrixSchedulerImOutbox::getFstatus, "PENDING", "RETRYING")
                .and(wrapper -> wrapper.isNull(MatrixSchedulerImOutbox::getFnextRetryTime)
                        .or()
                        .le(MatrixSchedulerImOutbox::getFnextRetryTime, now))) > 0;
    }

    private void deliver(MatrixSchedulerImOutbox task) {
        try {
            validateConfiguration();
            String timestamp = String.valueOf(System.currentTimeMillis());
            String nonce = UUID.randomUUID().toString().replace("-", "");
            String endpoint = imBaseUrl + SEND_PATH;
            String signature = sign(appSecret, endpoint, timestamp, nonce, task.getFpayload());
            String responseBody = restClient.post()
                    .uri(endpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-App-Code", appCode)
                    .header("X-Timestamp", timestamp)
                    .header("X-Nonce", nonce)
                    .header("X-Signature", signature)
                    .body(task.getFpayload())
                    .retrieve()
                    .body(String.class);
            JsonNode response = objectMapper.readTree(responseBody);
            if (response.path("code").asInt() != 200 && response.path("code").asInt() != 0) {
                throw new IllegalStateException("IM rejected notification: " + response.path("message").asText());
            }
            task.setFmessageNo(response.path("data").path("messageNo").asText(null));
            task.setFstatus("ACCEPTED");
            task.setFprocessingStartedTime(null);
            task.setFnextRetryTime(null);
            task.setFlastError(null);
            task.setFupdateTime(LocalDateTime.now());
            outboxMapper.updateById(task);
        } catch (Exception e) {
            int retryCount = task.getFretryCount() == null ? 1 : task.getFretryCount() + 1;
            task.setFretryCount(retryCount);
            task.setFstatus(retryCount >= maxRetryCount ? "DEAD" : "RETRYING");
            task.setFnextRetryTime(retryCount >= maxRetryCount ? null : nextRetryTime(retryCount));
            task.setFprocessingStartedTime(null);
            task.setFlastError(trim(safeMessage(e), 2000));
            task.setFupdateTime(LocalDateTime.now());
            outboxMapper.updateById(task);
        }
    }

    private String buildPayload(MatrixSchedulerAlertRecord alert, String requestId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("requestId", requestId);
        payload.put("messageType", "SCHEDULER_ALERT");
        payload.put("title", alert.getFtitle());
        payload.put("content", alert.getFcontent());
        payload.put("channels", channels);
        List<Map<String, Object>> recipients = new ArrayList<>();
        for (String userId : recipientUserIds) {
            recipients.add(Map.of("userId", userId));
        }
        payload.put("recipients", recipients);
        payload.put("business", Map.of(
                "type", "SCHEDULER_ALERT",
                "id", String.valueOf(alert.getFid()),
                "actionUrl", "/scheduler/operations?alertId=" + alert.getFid()
        ));
        payload.put("callbackUrl", callbackUrl);
        payload.put("tenantId", tenantId);
        payload.put("priority", "CRITICAL".equals(alert.getFlevel()) ? "URGENT" : "HIGH");
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize Scheduler IM payload", e);
        }
    }

    private void validateConfiguration() {
        if (!StringUtils.hasText(appSecret)) {
            throw new IllegalStateException("SCHEDULER_IM_APP_SECRET is required when Scheduler IM integration is enabled");
        }
        if (recipientUserIds.isEmpty()) {
            throw new IllegalStateException("SCHEDULER_IM_RECIPIENT_USER_IDS is required");
        }
        if (channels.isEmpty() || channels.stream().anyMatch(channel -> !"LOCAL".equalsIgnoreCase(channel))) {
            throw new IllegalStateException("Scheduler IM integration currently supports LOCAL channel only");
        }
        if (!StringUtils.hasText(callbackUrl) || !StringUtils.hasText(callbackSecret)) {
            throw new IllegalStateException("Scheduler IM callback URL and secret are required");
        }
    }

    private void validateCallbackSignature(String timestamp,
                                           String nonce,
                                           String signature,
                                           String rawBody) {
        if (!StringUtils.hasText(timestamp)
                || !StringUtils.hasText(nonce)
                || !StringUtils.hasText(signature)
                || !StringUtils.hasText(callbackSecret)) {
            throw new IllegalArgumentException("Missing IM callback signature headers");
        }
        long requestTime = Long.parseLong(timestamp);
        if (Math.abs(System.currentTimeMillis() - requestTime) > 300_000L) {
            throw new IllegalArgumentException("IM callback timestamp expired");
        }
        String expected = sign(callbackSecret, callbackUrl, timestamp, nonce, rawBody);
        if (!MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.toLowerCase().getBytes(StandardCharsets.UTF_8))) {
            throw new IllegalArgumentException("Invalid IM callback signature");
        }
    }

    public static String sign(String secret,
                              String url,
                              String timestamp,
                              String nonce,
                              String body) {
        try {
            String path = URI.create(url).getRawPath();
            String bodyHash = HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(body.getBytes(StandardCharsets.UTF_8)));
            String canonical = "POST\n" + path + "\n" + timestamp + "\n" + nonce + "\n" + bodyHash;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign Scheduler IM request", e);
        }
    }

    private LocalDateTime nextRetryTime(int retryCount) {
        long seconds = Math.min(3600L, 30L * (1L << Math.min(retryCount - 1, 6)));
        return LocalDateTime.now().plusSeconds(seconds);
    }

    private List<String> split(String csv) {
        if (!StringUtils.hasText(csv)) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    private String trimTrailingSlash(String value) {
        String result = value == null ? "" : value.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private String safeMessage(Exception e) {
        return e.getMessage() == null || e.getMessage().isBlank()
                ? e.getClass().getSimpleName()
                : e.getMessage();
    }

    private String trim(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
