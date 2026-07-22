package single.cjj.openapi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import single.cjj.openapi.contract.OpenVoucherDraftCreateResult;
import single.cjj.openapi.entity.OpenApiOutboxEvent;
import single.cjj.openapi.entity.OpenApiWriteRequest;
import single.cjj.openapi.entity.OpenApiWriteStatusLog;
import single.cjj.openapi.exception.OpenApiCallException;
import single.cjj.openapi.mapper.OpenApiOutboxEventMapper;
import single.cjj.openapi.mapper.OpenApiWriteRequestMapper;
import single.cjj.openapi.mapper.OpenApiWriteStatusLogMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class OpenApiWriteStateService {

    public static final String ACCEPTED = "ACCEPTED";
    public static final String PROCESSING = "PROCESSING";
    public static final String RETRYING = "RETRYING";
    public static final String PROCESSING_FAILED = "PROCESSING_FAILED";
    public static final String SUCCEEDED = "SUCCEEDED";
    public static final String MANUAL_REQUIRED = "MANUAL_REQUIRED";

    private static final Set<String> CLAIMABLE = Set.of(ACCEPTED, RETRYING, PROCESSING_FAILED);

    private final OpenApiWriteRequestMapper requestMapper;
    private final OpenApiOutboxEventMapper outboxMapper;
    private final OpenApiWriteStatusLogMapper statusLogMapper;
    private final int staleProcessingMinutes;

    public OpenApiWriteStateService(OpenApiWriteRequestMapper requestMapper,
                                    OpenApiOutboxEventMapper outboxMapper,
                                    OpenApiWriteStatusLogMapper statusLogMapper,
                                    @Value("${matrix.openapi.write.stale-processing-minutes:5}") int staleProcessingMinutes) {
        this.requestMapper = requestMapper;
        this.outboxMapper = outboxMapper;
        this.statusLogMapper = statusLogMapper;
        this.staleProcessingMinutes = Math.max(1, staleProcessingMinutes);
    }

    @Transactional
    public void initialize(OpenApiWriteRequest request) {
        createOutbox(request, LocalDateTime.now());
        appendLog(request.getId(), null, ACCEPTED, null, "写入请求已受理");
    }

    @Transactional
    public boolean claimForProcessing(Long writeRequestId) {
        OpenApiWriteRequest current = requestMapper.selectById(writeRequestId);
        if (current == null || SUCCEEDED.equals(current.getStatus())) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        int updated = requestMapper.update(null, new LambdaUpdateWrapper<OpenApiWriteRequest>()
                .eq(OpenApiWriteRequest::getId, writeRequestId)
                .in(OpenApiWriteRequest::getStatus, CLAIMABLE)
                .set(OpenApiWriteRequest::getStatus, PROCESSING)
                .set(OpenApiWriteRequest::getErrorCode, null)
                .set(OpenApiWriteRequest::getErrorMessage, null)
                .set(OpenApiWriteRequest::getUpdatedAt, now));
        if (updated > 0) {
            appendLog(writeRequestId, current.getStatus(), PROCESSING, null, "开始创建凭证草稿");
            return true;
        }
        return false;
    }

    @Transactional
    public void markSucceeded(Long writeRequestId, OpenVoucherDraftCreateResult result) {
        OpenApiWriteRequest current = require(writeRequestId);
        LocalDateTime now = LocalDateTime.now();
        int updated = requestMapper.update(null, new LambdaUpdateWrapper<OpenApiWriteRequest>()
                .eq(OpenApiWriteRequest::getId, writeRequestId)
                .ne(OpenApiWriteRequest::getStatus, SUCCEEDED)
                .set(OpenApiWriteRequest::getStatus, SUCCEEDED)
                .set(OpenApiWriteRequest::getVoucherId, result.getVoucherId())
                .set(OpenApiWriteRequest::getVoucherNumber, result.getVoucherNumber())
                .set(OpenApiWriteRequest::getErrorCode, null)
                .set(OpenApiWriteRequest::getErrorMessage, null)
                .set(OpenApiWriteRequest::getNextRetryAt, null)
                .set(OpenApiWriteRequest::getUpdatedAt, now)
                .set(OpenApiWriteRequest::getFinishedAt, now));
        if (updated > 0) {
            appendLog(writeRequestId, current.getStatus(), SUCCEEDED, null, "凭证草稿创建成功");
        }
    }

    @Transactional
    public void markFailed(Long writeRequestId, String errorCode, String errorMessage) {
        OpenApiWriteRequest current = require(writeRequestId);
        if (SUCCEEDED.equals(current.getStatus())) {
            return;
        }
        int nextRetryCount = (current.getRetryCount() == null ? 0 : current.getRetryCount()) + 1;
        int maxRetry = current.getMaxRetry() == null ? 5 : current.getMaxRetry();
        boolean retryable = nextRetryCount < maxRetry;
        String nextStatus = retryable ? RETRYING : MANUAL_REQUIRED;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextRetryAt = retryable ? now.plusMinutes(retryDelayMinutes(nextRetryCount)) : null;

        requestMapper.update(null, new LambdaUpdateWrapper<OpenApiWriteRequest>()
                .eq(OpenApiWriteRequest::getId, writeRequestId)
                .set(OpenApiWriteRequest::getStatus, nextStatus)
                .set(OpenApiWriteRequest::getRetryCount, nextRetryCount)
                .set(OpenApiWriteRequest::getErrorCode, errorCode)
                .set(OpenApiWriteRequest::getErrorMessage, truncate(errorMessage, 1000))
                .set(OpenApiWriteRequest::getNextRetryAt, nextRetryAt)
                .set(OpenApiWriteRequest::getUpdatedAt, now));
        appendLog(writeRequestId, current.getStatus(), nextStatus, errorCode, truncate(errorMessage, 1000));
        if (retryable) {
            OpenApiWriteRequest latest = requestMapper.selectById(writeRequestId);
            createOutbox(latest, nextRetryAt);
        }
    }

    @Transactional
    public boolean manualRetry(String requestId) {
        OpenApiWriteRequest current = requestMapper.selectOne(new LambdaQueryWrapper<OpenApiWriteRequest>()
                .eq(OpenApiWriteRequest::getRequestId, requestId));
        if (current == null) {
            throw new OpenApiCallException("OPENAPI_40401", "写入任务不存在", 404);
        }
        if (SUCCEEDED.equals(current.getStatus())) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        requestMapper.update(null, new LambdaUpdateWrapper<OpenApiWriteRequest>()
                .eq(OpenApiWriteRequest::getId, current.getId())
                .set(OpenApiWriteRequest::getStatus, RETRYING)
                .set(OpenApiWriteRequest::getErrorCode, null)
                .set(OpenApiWriteRequest::getErrorMessage, null)
                .set(OpenApiWriteRequest::getNextRetryAt, now)
                .set(OpenApiWriteRequest::getUpdatedAt, now));
        appendLog(current.getId(), current.getStatus(), RETRYING, null, "管理员触发重新执行");
        createOutbox(current, now);
        return true;
    }

    @Transactional
    public int recoverStaleProcessing() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(staleProcessingMinutes);
        List<OpenApiWriteRequest> stale = requestMapper.selectList(new LambdaQueryWrapper<OpenApiWriteRequest>()
                .eq(OpenApiWriteRequest::getStatus, PROCESSING)
                .lt(OpenApiWriteRequest::getUpdatedAt, cutoff)
                .orderByAsc(OpenApiWriteRequest::getId)
                .last("LIMIT 100"));
        int recovered = 0;
        for (OpenApiWriteRequest request : stale) {
            LocalDateTime now = LocalDateTime.now();
            int updated = requestMapper.update(null, new LambdaUpdateWrapper<OpenApiWriteRequest>()
                    .eq(OpenApiWriteRequest::getId, request.getId())
                    .eq(OpenApiWriteRequest::getStatus, PROCESSING)
                    .set(OpenApiWriteRequest::getStatus, RETRYING)
                    .set(OpenApiWriteRequest::getNextRetryAt, now)
                    .set(OpenApiWriteRequest::getUpdatedAt, now));
            if (updated > 0) {
                appendLog(request.getId(), PROCESSING, RETRYING, "OPENAPI_VOUCHER_50002", "处理超时，系统自动恢复");
                createOutbox(request, now);
                recovered++;
            }
        }
        return recovered;
    }

    private OpenApiWriteRequest require(Long id) {
        OpenApiWriteRequest request = requestMapper.selectById(id);
        if (request == null) {
            throw new OpenApiCallException("OPENAPI_40401", "写入任务不存在", 404);
        }
        return request;
    }

    private void createOutbox(OpenApiWriteRequest request, LocalDateTime nextAttemptAt) {
        LocalDateTime now = LocalDateTime.now();
        OpenApiOutboxEvent event = new OpenApiOutboxEvent();
        event.setEventId("evt_" + UUID.randomUUID().toString().replace("-", ""));
        event.setAggregateType("VOUCHER_WRITE_REQUEST");
        event.setAggregateId(request.getId());
        event.setEventType("VOUCHER_DRAFT_CREATE");
        event.setPayloadJson("{\"requestId\":\"" + request.getRequestId() + "\"}");
        event.setStatus("PENDING");
        event.setRetryCount(0);
        event.setMaxRetry(10);
        event.setNextAttemptAt(nextAttemptAt == null ? now : nextAttemptAt);
        event.setCreatedAt(now);
        event.setUpdatedAt(now);
        outboxMapper.insert(event);
    }

    private void appendLog(Long writeRequestId,
                           String fromStatus,
                           String toStatus,
                           String errorCode,
                           String message) {
        OpenApiWriteStatusLog log = new OpenApiWriteStatusLog();
        log.setWriteRequestId(writeRequestId);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setErrorCode(errorCode);
        log.setMessage(truncate(message, 1000));
        log.setCreatedAt(LocalDateTime.now());
        statusLogMapper.insert(log);
    }

    private long retryDelayMinutes(int retryCount) {
        return switch (retryCount) {
            case 1 -> 1;
            case 2 -> 5;
            case 3 -> 15;
            case 4 -> 60;
            default -> 180;
        };
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
