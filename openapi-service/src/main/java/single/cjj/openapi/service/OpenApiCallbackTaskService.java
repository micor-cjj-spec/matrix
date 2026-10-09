package single.cjj.openapi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import single.cjj.openapi.entity.OpenApiApp;
import single.cjj.openapi.entity.OpenApiCallbackTask;
import single.cjj.openapi.entity.OpenApiWriteRequest;
import single.cjj.openapi.exception.OpenApiCallException;
import single.cjj.openapi.mapper.OpenApiAppMapper;
import single.cjj.openapi.mapper.OpenApiCallbackTaskMapper;
import single.cjj.openapi.mapper.OpenApiWriteRequestMapper;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class OpenApiCallbackTaskService {

    private static final Set<String> TERMINAL_STATUSES = Set.of("SUCCEEDED", "MANUAL_REQUIRED");
    private static final Set<String> MANUALLY_RETRYABLE = Set.of("FAILED", "DEAD");

    private final OpenApiWriteRequestMapper writeRequestMapper;
    private final OpenApiCallbackTaskMapper callbackTaskMapper;
    private final OpenApiAppMapper appMapper;
    private final ObjectMapper objectMapper;

    public OpenApiCallbackTaskService(OpenApiWriteRequestMapper writeRequestMapper,
                                      OpenApiCallbackTaskMapper callbackTaskMapper,
                                      OpenApiAppMapper appMapper,
                                      ObjectMapper objectMapper) {
        this.writeRequestMapper = writeRequestMapper;
        this.callbackTaskMapper = callbackTaskMapper;
        this.appMapper = appMapper;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${matrix.openapi.callback.materialize-poll-ms:5000}")
    public void materializeTerminalCallbacks() {
        List<OpenApiWriteRequest> requests = writeRequestMapper.selectList(
                new LambdaQueryWrapper<OpenApiWriteRequest>()
                        .in(OpenApiWriteRequest::getStatus, TERMINAL_STATUSES)
                        .orderByDesc(OpenApiWriteRequest::getUpdatedAt)
                        .last("LIMIT 100")
        );
        for (OpenApiWriteRequest request : requests) {
            materialize(request);
        }
    }

    @Transactional
    public boolean materialize(OpenApiWriteRequest request) {
        if (request == null || !TERMINAL_STATUSES.contains(request.getStatus())) {
            return false;
        }
        OpenApiApp app = appMapper.selectById(request.getAppId());
        if (app == null || !Boolean.TRUE.equals(app.getCallbackEnabled())
                || !StringUtils.hasText(app.getCallbackUrl())) {
            return false;
        }
        String eventType = "SUCCEEDED".equals(request.getStatus())
                ? "VOUCHER_WRITE_SUCCEEDED"
                : "VOUCHER_WRITE_MANUAL_REQUIRED";
        Long count = callbackTaskMapper.selectCount(new LambdaQueryWrapper<OpenApiCallbackTask>()
                .eq(OpenApiCallbackTask::getWriteRequestId, request.getId())
                .eq(OpenApiCallbackTask::getEventType, eventType));
        if (count != null && count > 0) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        OpenApiCallbackTask task = new OpenApiCallbackTask();
        task.setEventId("cb_" + UUID.randomUUID().toString().replace("-", ""));
        task.setWriteRequestId(request.getId());
        task.setRequestId(request.getRequestId());
        task.setAppId(request.getAppId());
        task.setCallbackUrl(app.getCallbackUrl());
        task.setEventType(eventType);
        task.setPayloadJson(payload(request, eventType));
        task.setStatus("PENDING");
        task.setRetryCount(0);
        task.setMaxRetry(6);
        task.setNextAttemptAt(now);
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        try {
            callbackTaskMapper.insert(task);
            return true;
        } catch (DuplicateKeyException ignored) {
            return false;
        }
    }

    @Transactional
    public boolean manualRetry(String eventId) {
        OpenApiCallbackTask task = callbackTaskMapper.selectOne(
                new LambdaQueryWrapper<OpenApiCallbackTask>().eq(OpenApiCallbackTask::getEventId, eventId)
        );
        if (task == null) {
            throw new OpenApiCallException("OPENAPI_40401", "回调任务不存在", 404);
        }
        if (!MANUALLY_RETRYABLE.contains(task.getStatus())) {
            throw new OpenApiCallException("OPENAPI_CALLBACK_40901", "只有失败或死信回调可以重试", 409);
        }
        LocalDateTime now = LocalDateTime.now();
        int updated = callbackTaskMapper.update(null, new LambdaUpdateWrapper<OpenApiCallbackTask>()
                .eq(OpenApiCallbackTask::getId, task.getId())
                .in(OpenApiCallbackTask::getStatus, MANUALLY_RETRYABLE)
                .set(OpenApiCallbackTask::getStatus, "PENDING")
                .set(OpenApiCallbackTask::getRetryCount, 0)
                .set(OpenApiCallbackTask::getNextAttemptAt, now)
                .set(OpenApiCallbackTask::getErrorMessage, null)
                .set(OpenApiCallbackTask::getUpdatedAt, now));
        return updated > 0;
    }

    private String payload(OpenApiWriteRequest request, String eventType) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventType", eventType);
        payload.put("requestId", request.getRequestId());
        payload.put("externalBizNo", request.getExternalBizNo());
        payload.put("status", request.getStatus());
        payload.put("voucherId", request.getVoucherId());
        payload.put("voucherNumber", request.getVoucherNumber());
        payload.put("errorCode", request.getErrorCode());
        payload.put("errorMessage", request.getErrorMessage());
        payload.put("finishedAt", request.getFinishedAt());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("构造回调报文失败", e);
        }
    }
}
