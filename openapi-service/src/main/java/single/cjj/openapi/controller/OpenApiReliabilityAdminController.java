package single.cjj.openapi.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.openapi.contract.OpenApiPageResponse;
import single.cjj.openapi.entity.OpenApiApp;
import single.cjj.openapi.entity.OpenApiCallbackTask;
import single.cjj.openapi.entity.OpenApiReconcileRecord;
import single.cjj.openapi.exception.OpenApiCallException;
import single.cjj.openapi.mapper.OpenApiAppMapper;
import single.cjj.openapi.mapper.OpenApiCallbackTaskMapper;
import single.cjj.openapi.mapper.OpenApiReconcileRecordMapper;
import single.cjj.openapi.service.OpenApiCallbackTaskService;
import single.cjj.openapi.service.OpenApiCallbackUrlValidator;
import single.cjj.openapi.service.OpenApiReconciliationService;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/openapi/admin")
public class OpenApiReliabilityAdminController {

    private final OpenApiAppMapper appMapper;
    private final OpenApiCallbackTaskMapper callbackTaskMapper;
    private final OpenApiReconcileRecordMapper reconcileRecordMapper;
    private final OpenApiCallbackTaskService callbackTaskService;
    private final OpenApiReconciliationService reconciliationService;
    private final OpenApiCallbackUrlValidator callbackUrlValidator;

    public OpenApiReliabilityAdminController(OpenApiAppMapper appMapper,
                                             OpenApiCallbackTaskMapper callbackTaskMapper,
                                             OpenApiReconcileRecordMapper reconcileRecordMapper,
                                             OpenApiCallbackTaskService callbackTaskService,
                                             OpenApiReconciliationService reconciliationService,
                                             OpenApiCallbackUrlValidator callbackUrlValidator) {
        this.appMapper = appMapper;
        this.callbackTaskMapper = callbackTaskMapper;
        this.reconcileRecordMapper = reconcileRecordMapper;
        this.callbackTaskService = callbackTaskService;
        this.reconciliationService = reconciliationService;
        this.callbackUrlValidator = callbackUrlValidator;
    }

    @PutMapping("/apps/{id}/callback")
    public ApiResponse<Boolean> updateCallbackSettings(
            @PathVariable("id") Long id,
            @RequestBody CallbackSettingsRequest request) {
        OpenApiApp app = appMapper.selectById(id);
        if (app == null) {
            throw new OpenApiCallException("OPENAPI_40401", "应用不存在", 404);
        }
        if (request == null) {
            throw new OpenApiCallException("OPENAPI_CALLBACK_40001", "回调配置不能为空", 400);
        }
        String callbackUrl = callbackUrlValidator.validateAndNormalize(request.callbackUrl());
        boolean enabled = Boolean.TRUE.equals(request.enabled());
        if (enabled && !StringUtils.hasText(callbackUrl)) {
            throw new OpenApiCallException("OPENAPI_CALLBACK_40001", "启用回调时必须配置回调地址", 400);
        }
        app.setCallbackEnabled(enabled);
        app.setCallbackUrl(callbackUrl);
        app.setUpdatedAt(LocalDateTime.now());
        return ApiResponse.success(appMapper.updateById(app) > 0);
    }

    @GetMapping("/callbacks")
    public ApiResponse<OpenApiPageResponse<OpenApiCallbackTask>> listCallbacks(
            @RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(value = "eventId", required = false) String eventId,
            @RequestParam(value = "requestId", required = false) String requestId,
            @RequestParam(value = "appId", required = false) Long appId,
            @RequestParam(value = "status", required = false) String status) {
        int safePageNo = Math.max(1, pageNo);
        int safePageSize = Math.max(1, Math.min(pageSize, 200));
        LambdaQueryWrapper<OpenApiCallbackTask> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(eventId)) {
            wrapper.eq(OpenApiCallbackTask::getEventId, eventId.trim());
        }
        if (StringUtils.hasText(requestId)) {
            wrapper.eq(OpenApiCallbackTask::getRequestId, requestId.trim());
        }
        if (appId != null) {
            wrapper.eq(OpenApiCallbackTask::getAppId, appId);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(OpenApiCallbackTask::getStatus, status.trim().toUpperCase());
        }
        wrapper.orderByDesc(OpenApiCallbackTask::getCreatedAt)
                .orderByDesc(OpenApiCallbackTask::getId);
        IPage<OpenApiCallbackTask> page = callbackTaskMapper.selectPage(
                new Page<>(safePageNo, safePageSize), wrapper
        );
        return ApiResponse.success(OpenApiPageResponse.of(
                page.getTotal(), safePageNo, safePageSize, page.getRecords()
        ));
    }

    @PostMapping("/callbacks/{eventId}/retry")
    public ApiResponse<Boolean> retryCallback(@PathVariable("eventId") String eventId) {
        return ApiResponse.success(callbackTaskService.manualRetry(eventId));
    }

    @GetMapping("/reconciliation")
    public ApiResponse<OpenApiPageResponse<OpenApiReconcileRecord>> listReconciliation(
            @RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(value = "recordId", required = false) String recordId,
            @RequestParam(value = "requestId", required = false) String requestId,
            @RequestParam(value = "issueType", required = false) String issueType,
            @RequestParam(value = "severity", required = false) String severity,
            @RequestParam(value = "status", required = false) String status) {
        int safePageNo = Math.max(1, pageNo);
        int safePageSize = Math.max(1, Math.min(pageSize, 200));
        LambdaQueryWrapper<OpenApiReconcileRecord> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(recordId)) {
            wrapper.eq(OpenApiReconcileRecord::getRecordId, recordId.trim());
        }
        if (StringUtils.hasText(requestId)) {
            wrapper.eq(OpenApiReconcileRecord::getRequestId, requestId.trim());
        }
        if (StringUtils.hasText(issueType)) {
            wrapper.eq(OpenApiReconcileRecord::getIssueType, issueType.trim().toUpperCase());
        }
        if (StringUtils.hasText(severity)) {
            wrapper.eq(OpenApiReconcileRecord::getSeverity, severity.trim().toUpperCase());
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(OpenApiReconcileRecord::getStatus, status.trim().toUpperCase());
        }
        wrapper.orderByDesc(OpenApiReconcileRecord::getDetectedAt)
                .orderByDesc(OpenApiReconcileRecord::getId);
        IPage<OpenApiReconcileRecord> page = reconcileRecordMapper.selectPage(
                new Page<>(safePageNo, safePageSize), wrapper
        );
        return ApiResponse.success(OpenApiPageResponse.of(
                page.getTotal(), safePageNo, safePageSize, page.getRecords()
        ));
    }

    @PostMapping("/reconciliation/run")
    public ApiResponse<OpenApiReconciliationService.ReconcileSummary> runReconciliation(
            @RequestParam(value = "lookbackDays", defaultValue = "7") int lookbackDays) {
        return ApiResponse.success(reconciliationService.run(lookbackDays));
    }

    @PostMapping("/reconciliation/{recordId}/repair")
    public ApiResponse<Boolean> repair(
            @PathVariable("recordId") String recordId,
            @RequestHeader(value = "X-User-Id", required = false) String operator) {
        return ApiResponse.success(reconciliationService.repair(recordId, operator));
    }

    @PostMapping("/reconciliation/{recordId}/resolve")
    public ApiResponse<Boolean> resolve(
            @PathVariable("recordId") String recordId,
            @RequestHeader(value = "X-User-Id", required = false) String operator,
            @RequestBody(required = false) ResolveRequest request) {
        return ApiResponse.success(reconciliationService.manualResolve(
                recordId, operator, request == null ? null : request.resolution()
        ));
    }

    public record CallbackSettingsRequest(Boolean enabled, String callbackUrl) {
    }

    public record ResolveRequest(String resolution) {
    }
}
