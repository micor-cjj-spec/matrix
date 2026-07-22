package single.cjj.openapi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import single.cjj.openapi.entity.OpenApiApp;
import single.cjj.openapi.entity.OpenApiCallbackTask;
import single.cjj.openapi.mapper.OpenApiAppMapper;
import single.cjj.openapi.mapper.OpenApiCallbackTaskMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
public class OpenApiCallbackDispatcher {

    private static final Set<String> DISPATCHABLE = Set.of("PENDING", "FAILED");

    private final OpenApiCallbackTaskMapper callbackTaskMapper;
    private final OpenApiAppMapper appMapper;
    private final OpenApiSecretService secretService;
    private final OpenApiCallbackSignatureService signatureService;
    private final OpenApiCallbackUrlValidator urlValidator;
    private final HttpClient httpClient;
    private final int requestTimeoutSeconds;

    public OpenApiCallbackDispatcher(OpenApiCallbackTaskMapper callbackTaskMapper,
                                     OpenApiAppMapper appMapper,
                                     OpenApiSecretService secretService,
                                     OpenApiCallbackSignatureService signatureService,
                                     OpenApiCallbackUrlValidator urlValidator,
                                     @Value("${matrix.openapi.callback.connect-timeout-seconds:3}") int connectTimeoutSeconds,
                                     @Value("${matrix.openapi.callback.request-timeout-seconds:5}") int requestTimeoutSeconds) {
        this.callbackTaskMapper = callbackTaskMapper;
        this.appMapper = appMapper;
        this.secretService = secretService;
        this.signatureService = signatureService;
        this.urlValidator = urlValidator;
        this.requestTimeoutSeconds = Math.max(1, requestTimeoutSeconds);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.max(1, connectTimeoutSeconds)))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @Scheduled(fixedDelayString = "${matrix.openapi.callback.dispatch-poll-ms:3000}")
    public void dispatch() {
        LocalDateTime now = LocalDateTime.now();
        List<OpenApiCallbackTask> tasks = callbackTaskMapper.selectList(
                new LambdaQueryWrapper<OpenApiCallbackTask>()
                        .in(OpenApiCallbackTask::getStatus, DISPATCHABLE)
                        .le(OpenApiCallbackTask::getNextAttemptAt, now)
                        .orderByAsc(OpenApiCallbackTask::getId)
                        .last("LIMIT 50")
        );
        for (OpenApiCallbackTask task : tasks) {
            dispatchOne(task);
        }
    }

    private void dispatchOne(OpenApiCallbackTask task) {
        LocalDateTime now = LocalDateTime.now();
        int claimed = callbackTaskMapper.update(null, new LambdaUpdateWrapper<OpenApiCallbackTask>()
                .eq(OpenApiCallbackTask::getId, task.getId())
                .in(OpenApiCallbackTask::getStatus, DISPATCHABLE)
                .set(OpenApiCallbackTask::getStatus, "SENDING")
                .set(OpenApiCallbackTask::getUpdatedAt, now));
        if (claimed == 0) {
            return;
        }

        try {
            OpenApiApp app = appMapper.selectById(task.getAppId());
            if (app == null || !Boolean.TRUE.equals(app.getCallbackEnabled())) {
                markSkipped(task, "应用回调已关闭");
                return;
            }
            String callbackUrl = urlValidator.validateAndNormalize(task.getCallbackUrl());
            String timestamp = String.valueOf(System.currentTimeMillis());
            String nonce = UUID.randomUUID().toString().replace("-", "");
            String secret = secretService.decrypt(app.getAppSecretCipher());
            String signature = signatureService.sign(
                    secret, task.getEventId(), timestamp, nonce, task.getPayloadJson()
            );

            HttpRequest request = HttpRequest.newBuilder(URI.create(callbackUrl))
                    .timeout(Duration.ofSeconds(requestTimeoutSeconds))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "Matrix-OpenAPI-Callback/1.0")
                    .header("X-Matrix-Callback-Event-Id", task.getEventId())
                    .header("X-Matrix-Timestamp", timestamp)
                    .header("X-Matrix-Nonce", nonce)
                    .header("X-Matrix-Signature", signature)
                    .POST(HttpRequest.BodyPublishers.ofString(task.getPayloadJson()))
                    .build();
            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString()
            );
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                markSucceeded(task, response.statusCode());
            } else {
                markFailed(task, response.statusCode(), "回调HTTP状态异常: " + response.statusCode());
            }
        } catch (Exception e) {
            markFailed(task, null, e.getMessage());
        }
    }

    private void markSucceeded(OpenApiCallbackTask task, int httpStatus) {
        LocalDateTime now = LocalDateTime.now();
        callbackTaskMapper.update(null, new LambdaUpdateWrapper<OpenApiCallbackTask>()
                .eq(OpenApiCallbackTask::getId, task.getId())
                .eq(OpenApiCallbackTask::getStatus, "SENDING")
                .set(OpenApiCallbackTask::getStatus, "SUCCEEDED")
                .set(OpenApiCallbackTask::getLastHttpStatus, httpStatus)
                .set(OpenApiCallbackTask::getErrorMessage, null)
                .set(OpenApiCallbackTask::getSentAt, now)
                .set(OpenApiCallbackTask::getUpdatedAt, now));
    }

    private void markSkipped(OpenApiCallbackTask task, String message) {
        callbackTaskMapper.update(null, new LambdaUpdateWrapper<OpenApiCallbackTask>()
                .eq(OpenApiCallbackTask::getId, task.getId())
                .eq(OpenApiCallbackTask::getStatus, "SENDING")
                .set(OpenApiCallbackTask::getStatus, "SKIPPED")
                .set(OpenApiCallbackTask::getErrorMessage, truncate(message, 1000))
                .set(OpenApiCallbackTask::getUpdatedAt, LocalDateTime.now()));
    }

    private void markFailed(OpenApiCallbackTask task, Integer httpStatus, String message) {
        int retryCount = (task.getRetryCount() == null ? 0 : task.getRetryCount()) + 1;
        int maxRetry = task.getMaxRetry() == null ? 6 : task.getMaxRetry();
        boolean exhausted = retryCount >= maxRetry;
        LocalDateTime now = LocalDateTime.now();
        callbackTaskMapper.update(null, new LambdaUpdateWrapper<OpenApiCallbackTask>()
                .eq(OpenApiCallbackTask::getId, task.getId())
                .eq(OpenApiCallbackTask::getStatus, "SENDING")
                .set(OpenApiCallbackTask::getStatus, exhausted ? "DEAD" : "FAILED")
                .set(OpenApiCallbackTask::getRetryCount, retryCount)
                .set(OpenApiCallbackTask::getNextAttemptAt,
                        exhausted ? null : now.plusMinutes(retryDelayMinutes(retryCount)))
                .set(OpenApiCallbackTask::getLastHttpStatus, httpStatus)
                .set(OpenApiCallbackTask::getErrorMessage, truncate(message, 1000))
                .set(OpenApiCallbackTask::getUpdatedAt, now));
        log.warn("OpenAPI callback failed, eventId={}, retry={}, message={}",
                task.getEventId(), retryCount, message);
    }

    private long retryDelayMinutes(int retryCount) {
        return switch (retryCount) {
            case 1 -> 1;
            case 2 -> 5;
            case 3 -> 15;
            case 4 -> 60;
            case 5 -> 180;
            default -> 360;
        };
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
