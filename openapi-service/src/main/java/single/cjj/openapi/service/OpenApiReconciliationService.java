package single.cjj.openapi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.openapi.client.FiVoucherWriteClient;
import single.cjj.openapi.contract.OpenVoucherDraftCreateResult;
import single.cjj.openapi.entity.OpenApiCallbackTask;
import single.cjj.openapi.entity.OpenApiOutboxEvent;
import single.cjj.openapi.entity.OpenApiReconcileRecord;
import single.cjj.openapi.entity.OpenApiWriteRequest;
import single.cjj.openapi.exception.OpenApiCallException;
import single.cjj.openapi.mapper.OpenApiCallbackTaskMapper;
import single.cjj.openapi.mapper.OpenApiOutboxEventMapper;
import single.cjj.openapi.mapper.OpenApiReconcileRecordMapper;
import single.cjj.openapi.mapper.OpenApiWriteRequestMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class OpenApiReconciliationService {

    private final OpenApiWriteRequestMapper writeRequestMapper;
    private final OpenApiOutboxEventMapper outboxMapper;
    private final OpenApiCallbackTaskMapper callbackTaskMapper;
    private final OpenApiReconcileRecordMapper reconcileRecordMapper;
    private final FiVoucherWriteClient fiVoucherWriteClient;
    private final OpenApiWriteStateService writeStateService;
    private final OpenApiCallbackTaskService callbackTaskService;
    private final int defaultLookbackDays;
    private final int stuckMinutes;

    public OpenApiReconciliationService(OpenApiWriteRequestMapper writeRequestMapper,
                                        OpenApiOutboxEventMapper outboxMapper,
                                        OpenApiCallbackTaskMapper callbackTaskMapper,
                                        OpenApiReconcileRecordMapper reconcileRecordMapper,
                                        FiVoucherWriteClient fiVoucherWriteClient,
                                        OpenApiWriteStateService writeStateService,
                                        OpenApiCallbackTaskService callbackTaskService,
                                        @Value("${matrix.openapi.reconcile.lookback-days:7}") int defaultLookbackDays,
                                        @Value("${matrix.openapi.reconcile.stuck-minutes:10}") int stuckMinutes) {
        this.writeRequestMapper = writeRequestMapper;
        this.outboxMapper = outboxMapper;
        this.callbackTaskMapper = callbackTaskMapper;
        this.reconcileRecordMapper = reconcileRecordMapper;
        this.fiVoucherWriteClient = fiVoucherWriteClient;
        this.writeStateService = writeStateService;
        this.callbackTaskService = callbackTaskService;
        this.defaultLookbackDays = Math.max(1, Math.min(defaultLookbackDays, 90));
        this.stuckMinutes = Math.max(1, stuckMinutes);
    }

    @Scheduled(cron = "${matrix.openapi.reconcile.cron:0 30 2 * * ?}")
    public void scheduledRun() {
        run(defaultLookbackDays);
    }

    public ReconcileSummary run(int lookbackDays) {
        int safeDays = Math.max(1, Math.min(lookbackDays, 90));
        LocalDateTime from = LocalDateTime.now().minusDays(safeDays);
        List<OpenApiWriteRequest> requests = writeRequestMapper.selectList(
                new LambdaQueryWrapper<OpenApiWriteRequest>()
                        .ge(OpenApiWriteRequest::getCreatedAt, from)
                        .orderByAsc(OpenApiWriteRequest::getId)
                        .last("LIMIT 5000")
        );
        int issues = 0;
        for (OpenApiWriteRequest request : requests) {
            issues += reconcileWriteRequest(request);
        }
        issues += reconcileOutbox(from);
        issues += reconcileCallbacks(from);
        return new ReconcileSummary(safeDays, requests.size(), issues, LocalDateTime.now());
    }

    private int reconcileWriteRequest(OpenApiWriteRequest request) {
        OpenVoucherDraftCreateResult actual = null;
        try {
            ApiResponse<OpenVoucherDraftCreateResult> response = fiVoucherWriteClient.findBySourceRequest(
                    request.getRequestId(), request.getTenantId()
            );
            if (response != null && response.getCode() == 200) {
                actual = response.getData();
            }
        } catch (Exception e) {
            upsertIssue(
                    "FINANCE_LOOKUP_FAILED:" + request.getRequestId(),
                    "FINANCE_LOOKUP_FAILED", "WARNING", request,
                    request.getStatus(), "LOOKUP_FAILED", "财务服务核验失败: " + e.getMessage()
            );
            return 1;
        }
        resolveIfOpen("FINANCE_LOOKUP_FAILED:" + request.getRequestId(), "财务服务核验恢复正常");

        if ("SUCCEEDED".equals(request.getStatus()) && actual == null) {
            upsertIssue(
                    "VOUCHER_MISSING:" + request.getRequestId(),
                    "VOUCHER_MISSING", "CRITICAL", request,
                    "VOUCHER_EXISTS", "NOT_FOUND", "任务成功但财务凭证不存在"
            );
            return 1;
        }
        resolveIfOpen("VOUCHER_MISSING:" + request.getRequestId(), "财务凭证已存在");

        if (!"SUCCEEDED".equals(request.getStatus()) && actual != null) {
            upsertIssue(
                    "TASK_STATUS_MISMATCH:" + request.getRequestId(),
                    "TASK_STATUS_MISMATCH", "HIGH", request,
                    "SUCCEEDED", request.getStatus(), "财务凭证已存在，但写入任务未标记成功"
            );
            return 1;
        }
        resolveIfOpen("TASK_STATUS_MISMATCH:" + request.getRequestId(), "任务与凭证状态已一致");

        if (actual != null && request.getVoucherId() != null
                && !Objects.equals(request.getVoucherId(), actual.getVoucherId())) {
            upsertIssue(
                    "VOUCHER_ID_MISMATCH:" + request.getRequestId(),
                    "VOUCHER_ID_MISMATCH", "HIGH", request,
                    String.valueOf(request.getVoucherId()), String.valueOf(actual.getVoucherId()),
                    "任务记录的凭证ID与财务实际凭证不一致"
            );
            return 1;
        }
        resolveIfOpen("VOUCHER_ID_MISMATCH:" + request.getRequestId(), "凭证ID已一致");
        return 0;
    }

    private int reconcileOutbox(LocalDateTime from) {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(stuckMinutes);
        List<OpenApiOutboxEvent> events = outboxMapper.selectList(
                new LambdaQueryWrapper<OpenApiOutboxEvent>()
                        .ge(OpenApiOutboxEvent::getCreatedAt, from)
                        .in(OpenApiOutboxEvent::getStatus, "PENDING", "SENDING", "FAILED", "DEAD")
                        .orderByAsc(OpenApiOutboxEvent::getId)
                        .last("LIMIT 5000")
        );
        int issues = 0;
        for (OpenApiOutboxEvent event : events) {
            String issueKey = "OUTBOX_STUCK:" + event.getEventId();
            boolean stuck = "DEAD".equals(event.getStatus())
                    || event.getUpdatedAt() == null
                    || event.getUpdatedAt().isBefore(cutoff);
            if (stuck) {
                OpenApiWriteRequest request = writeRequestMapper.selectById(event.getAggregateId());
                upsertIssue(
                        issueKey, "OUTBOX_STUCK",
                        "DEAD".equals(event.getStatus()) ? "CRITICAL" : "HIGH",
                        request, "SENT", event.getStatus(),
                        "Outbox事件长时间未成功投递: " + event.getEventId()
                );
                issues++;
            } else {
                resolveIfOpen(issueKey, "Outbox事件已恢复");
            }
        }
        return issues;
    }

    private int reconcileCallbacks(LocalDateTime from) {
        List<OpenApiCallbackTask> tasks = callbackTaskMapper.selectList(
                new LambdaQueryWrapper<OpenApiCallbackTask>()
                        .ge(OpenApiCallbackTask::getCreatedAt, from)
                        .orderByAsc(OpenApiCallbackTask::getId)
                        .last("LIMIT 5000")
        );
        int issues = 0;
        for (OpenApiCallbackTask task : tasks) {
            String issueKey = "CALLBACK_DEAD:" + task.getEventId();
            if ("DEAD".equals(task.getStatus())) {
                OpenApiWriteRequest request = writeRequestMapper.selectById(task.getWriteRequestId());
                upsertIssue(
                        issueKey, "CALLBACK_DEAD", "HIGH", request,
                        "SUCCEEDED", task.getStatus(),
                        "回调超过最大重试次数: " + task.getEventId()
                );
                issues++;
            } else {
                resolveIfOpen(issueKey, "回调任务已恢复");
            }
        }
        return issues;
    }

    @Transactional
    public boolean repair(String recordId, String operator) {
        OpenApiReconcileRecord record = requireRecord(recordId);
        if (!"OPEN".equals(record.getStatus())) {
            throw new OpenApiCallException("OPENAPI_RECONCILE_40901", "只有未处理异常可以修复", 409);
        }
        if ("TASK_STATUS_MISMATCH".equals(record.getIssueType())) {
            OpenApiWriteRequest request = writeRequestMapper.selectById(record.getWriteRequestId());
            ApiResponse<OpenVoucherDraftCreateResult> response = fiVoucherWriteClient.findBySourceRequest(
                    request.getRequestId(), request.getTenantId()
            );
            if (response == null || response.getData() == null) {
                throw new OpenApiCallException("OPENAPI_RECONCILE_40902", "财务凭证仍不存在，无法修复任务状态", 409);
            }
            writeStateService.markSucceeded(request.getId(), response.getData());
            resolve(record, operator, "已按财务实际凭证修复写入任务状态");
            return true;
        }
        if ("OUTBOX_STUCK".equals(record.getIssueType())) {
            OpenApiOutboxEvent event = outboxMapper.selectOne(
                    new LambdaQueryWrapper<OpenApiOutboxEvent>()
                            .eq(OpenApiOutboxEvent::getAggregateId, record.getWriteRequestId())
                            .orderByDesc(OpenApiOutboxEvent::getId)
                            .last("LIMIT 1")
            );
            if (event == null) {
                throw new OpenApiCallException("OPENAPI_RECONCILE_40902", "Outbox事件不存在", 409);
            }
            outboxMapper.update(null, new LambdaUpdateWrapper<OpenApiOutboxEvent>()
                    .eq(OpenApiOutboxEvent::getId, event.getId())
                    .set(OpenApiOutboxEvent::getStatus, "FAILED")
                    .set(OpenApiOutboxEvent::getNextAttemptAt, LocalDateTime.now())
                    .set(OpenApiOutboxEvent::getErrorMessage, null)
                    .set(OpenApiOutboxEvent::getUpdatedAt, LocalDateTime.now()));
            resolve(record, operator, "已重新激活Outbox事件");
            return true;
        }
        if ("CALLBACK_DEAD".equals(record.getIssueType())) {
            String eventId = record.getIssueKey().substring("CALLBACK_DEAD:".length());
            callbackTaskService.manualRetry(eventId);
            resolve(record, operator, "已重新激活回调任务");
            return true;
        }
        throw new OpenApiCallException("OPENAPI_RECONCILE_40902", "该异常需要人工核查后手动关闭", 409);
    }

    @Transactional
    public boolean manualResolve(String recordId, String operator, String resolution) {
        OpenApiReconcileRecord record = requireRecord(recordId);
        resolve(record, operator, resolution == null ? "管理员手动关闭" : resolution);
        return true;
    }

    private void upsertIssue(String issueKey,
                             String issueType,
                             String severity,
                             OpenApiWriteRequest request,
                             String expectedStatus,
                             String actualStatus,
                             String detail) {
        OpenApiReconcileRecord record = reconcileRecordMapper.selectOne(
                new LambdaQueryWrapper<OpenApiReconcileRecord>()
                        .eq(OpenApiReconcileRecord::getIssueKey, issueKey)
        );
        LocalDateTime now = LocalDateTime.now();
        if (record == null) {
            record = new OpenApiReconcileRecord();
            record.setRecordId("rec_" + UUID.randomUUID().toString().replace("-", ""));
            record.setIssueKey(issueKey);
            record.setCreatedAt(now);
        }
        record.setIssueType(issueType);
        record.setSeverity(severity);
        record.setWriteRequestId(request == null ? null : request.getId());
        record.setRequestId(request == null ? null : request.getRequestId());
        record.setAppId(request == null ? null : request.getAppId());
        record.setExpectedStatus(expectedStatus);
        record.setActualStatus(actualStatus);
        record.setDetailMessage(truncate(detail, 1000));
        record.setStatus("OPEN");
        record.setResolution(null);
        record.setResolvedBy(null);
        record.setResolvedAt(null);
        record.setDetectedAt(now);
        record.setUpdatedAt(now);
        if (record.getId() == null) {
            reconcileRecordMapper.insert(record);
        } else {
            reconcileRecordMapper.updateById(record);
        }
    }

    private void resolveIfOpen(String issueKey, String resolution) {
        OpenApiReconcileRecord record = reconcileRecordMapper.selectOne(
                new LambdaQueryWrapper<OpenApiReconcileRecord>()
                        .eq(OpenApiReconcileRecord::getIssueKey, issueKey)
                        .eq(OpenApiReconcileRecord::getStatus, "OPEN")
        );
        if (record != null) {
            resolve(record, "system", resolution);
        }
    }

    private void resolve(OpenApiReconcileRecord record, String operator, String resolution) {
        LocalDateTime now = LocalDateTime.now();
        record.setStatus("RESOLVED");
        record.setResolution(truncate(resolution, 1000));
        record.setResolvedBy(operator == null ? "admin" : operator);
        record.setResolvedAt(now);
        record.setUpdatedAt(now);
        reconcileRecordMapper.updateById(record);
    }

    private OpenApiReconcileRecord requireRecord(String recordId) {
        OpenApiReconcileRecord record = reconcileRecordMapper.selectOne(
                new LambdaQueryWrapper<OpenApiReconcileRecord>()
                        .eq(OpenApiReconcileRecord::getRecordId, recordId)
        );
        if (record == null) {
            throw new OpenApiCallException("OPENAPI_40401", "对账异常不存在", 404);
        }
        return record;
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    public record ReconcileSummary(
            int lookbackDays,
            int scannedWriteRequests,
            int detectedIssues,
            LocalDateTime finishedAt) {
    }
}
