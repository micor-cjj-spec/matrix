package single.cjj.openapi.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.openapi.contract.OpenApiPageResponse;
import single.cjj.openapi.entity.OpenApiApp;
import single.cjj.openapi.entity.OpenApiGrant;
import single.cjj.openapi.entity.OpenApiRequestLog;
import single.cjj.openapi.exception.OpenApiCallException;
import single.cjj.openapi.mapper.OpenApiAppMapper;
import single.cjj.openapi.mapper.OpenApiGrantMapper;
import single.cjj.openapi.mapper.OpenApiRequestLogMapper;
import single.cjj.openapi.service.OpenApiSecretService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * OpenAPI 第二期治理能力。
 * 管理端接口继续由 Gateway JWT 认证保护。
 */
@RestController
@RequestMapping("/openapi/admin")
public class OpenApiGovernanceController {

    private static final int MAX_DASHBOARD_SAMPLE = 10000;

    private final OpenApiAppMapper appMapper;
    private final OpenApiGrantMapper grantMapper;
    private final OpenApiRequestLogMapper requestLogMapper;
    private final OpenApiSecretService secretService;

    public OpenApiGovernanceController(OpenApiAppMapper appMapper,
                                       OpenApiGrantMapper grantMapper,
                                       OpenApiRequestLogMapper requestLogMapper,
                                       OpenApiSecretService secretService) {
        this.appMapper = appMapper;
        this.grantMapper = grantMapper;
        this.requestLogMapper = requestLogMapper;
        this.secretService = secretService;
    }

    @PutMapping("/apps/{id}")
    public ApiResponse<Boolean> updateApp(@PathVariable("id") Long id,
                                          @RequestBody UpdateAppRequest request) {
        OpenApiApp app = requireApp(id);
        if (request == null) {
            throw badRequest("应用参数不能为空");
        }
        if (StringUtils.hasText(request.appName())) {
            app.setAppName(request.appName().trim());
        }
        app.setValidFrom(request.validFrom());
        app.setValidTo(request.validTo());
        app.setIpWhitelist(normalizeNullable(request.ipWhitelist()));
        if (request.qpsLimit() != null) {
            app.setQpsLimit(normalizePositive(request.qpsLimit(), 1, 10000, "QPS"));
        }
        if (request.maxPageSize() != null) {
            app.setMaxPageSize(normalizePositive(request.maxPageSize(), 1, 500, "分页上限"));
        }
        app.setUpdatedAt(LocalDateTime.now());
        return ApiResponse.success(appMapper.updateById(app) > 0);
    }

    @PostMapping("/apps/{id}/rotate-secret")
    public ApiResponse<RotateSecretResponse> rotateSecret(@PathVariable("id") Long id) {
        OpenApiApp app = requireApp(id);
        OpenApiSecretService.GeneratedSecret secret = secretService.generateSecret();
        app.setAppSecretCipher(secret.appSecretCipher());
        app.setUpdatedAt(LocalDateTime.now());
        appMapper.updateById(app);
        return ApiResponse.success(new RotateSecretResponse(
                app.getId(), app.getAppId(), app.getAppKey(), secret.appSecret(), app.getUpdatedAt()
        ));
    }

    @PutMapping("/grants/{id}")
    public ApiResponse<OpenApiGrant> updateGrant(@PathVariable("id") Long id,
                                                 @RequestBody UpdateGrantRequest request) {
        OpenApiGrant grant = requireGrant(id);
        if (request == null) {
            throw badRequest("授权参数不能为空");
        }
        if (StringUtils.hasText(request.status())) {
            grant.setStatus(normalizeGrantStatus(request.status()));
        }
        if (request.dataPermissionJson() != null) {
            grant.setDataPermissionJson(request.dataPermissionJson());
        }
        if (request.fieldPermissionJson() != null) {
            grant.setFieldPermissionJson(request.fieldPermissionJson());
        }
        grant.setValidFrom(request.validFrom());
        grant.setValidTo(request.validTo());
        grant.setUpdatedAt(LocalDateTime.now());
        grantMapper.updateById(grant);
        return ApiResponse.success(grant);
    }

    @DeleteMapping("/grants/{id}")
    public ApiResponse<Boolean> revokeGrant(@PathVariable("id") Long id) {
        OpenApiGrant grant = requireGrant(id);
        grant.setStatus("REVOKED");
        grant.setUpdatedAt(LocalDateTime.now());
        return ApiResponse.success(grantMapper.updateById(grant) > 0);
    }

    @GetMapping("/logs")
    public ApiResponse<OpenApiPageResponse<OpenApiRequestLog>> listLogs(
            @RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(value = "requestId", required = false) String requestId,
            @RequestParam(value = "appId", required = false) String appId,
            @RequestParam(value = "apiCode", required = false) String apiCode,
            @RequestParam(value = "clientIp", required = false) String clientIp,
            @RequestParam(value = "responseCode", required = false) String responseCode,
            @RequestParam(value = "success", required = false) Boolean success,
            @RequestParam(value = "startTime", required = false) LocalDateTime startTime,
            @RequestParam(value = "endTime", required = false) LocalDateTime endTime) {
        int safePageNo = Math.max(1, pageNo);
        int safePageSize = Math.max(1, Math.min(pageSize, 200));
        LambdaQueryWrapper<OpenApiRequestLog> wrapper = buildLogWrapper(
                requestId, appId, apiCode, clientIp, responseCode, success, startTime, endTime
        ).orderByDesc(OpenApiRequestLog::getRequestTime)
                .orderByDesc(OpenApiRequestLog::getId);
        IPage<OpenApiRequestLog> page = requestLogMapper.selectPage(
                new Page<>(safePageNo, safePageSize), wrapper
        );
        return ApiResponse.success(OpenApiPageResponse.of(
                page.getTotal(), safePageNo, safePageSize, page.getRecords()
        ));
    }

    @GetMapping("/logs/{requestId}")
    public ApiResponse<OpenApiRequestLog> getLog(@PathVariable("requestId") String requestId) {
        OpenApiRequestLog log = requestLogMapper.selectOne(
                new LambdaQueryWrapper<OpenApiRequestLog>()
                        .eq(OpenApiRequestLog::getRequestId, requestId)
        );
        if (log == null) {
            throw new OpenApiCallException("OPENAPI_40401", "调用日志不存在", 404);
        }
        return ApiResponse.success(log);
    }

    @GetMapping("/dashboard")
    public ApiResponse<DashboardView> dashboard(
            @RequestParam(value = "hours", defaultValue = "24") int hours,
            @RequestParam(value = "appId", required = false) String appId) {
        int safeHours = Math.max(1, Math.min(hours, 24 * 31));
        LocalDateTime from = LocalDateTime.now().minusHours(safeHours);
        LambdaQueryWrapper<OpenApiRequestLog> wrapper = new LambdaQueryWrapper<OpenApiRequestLog>()
                .ge(OpenApiRequestLog::getRequestTime, from)
                .orderByDesc(OpenApiRequestLog::getRequestTime)
                .last("LIMIT " + MAX_DASHBOARD_SAMPLE);
        if (StringUtils.hasText(appId)) {
            wrapper.eq(OpenApiRequestLog::getAppId, appId.trim());
        }
        List<OpenApiRequestLog> logs = requestLogMapper.selectList(wrapper);

        long total = logs.size();
        long successCount = logs.stream().filter(item -> Boolean.TRUE.equals(item.getSuccess())).count();
        long failureCount = total - successCount;
        long averageDurationMs = Math.round(logs.stream()
                .filter(item -> item.getDurationMs() != null)
                .mapToLong(OpenApiRequestLog::getDurationMs)
                .average()
                .orElse(0));
        long p95DurationMs = percentile95(logs);
        double successRate = total == 0 ? 0D : round2(successCount * 100D / total);

        return ApiResponse.success(new DashboardView(
                safeHours,
                total,
                successCount,
                failureCount,
                successRate,
                averageDurationMs,
                p95DurationMs,
                countTopApis(logs),
                countErrorCodes(logs),
                total >= MAX_DASHBOARD_SAMPLE
        ));
    }

    private LambdaQueryWrapper<OpenApiRequestLog> buildLogWrapper(
            String requestId,
            String appId,
            String apiCode,
            String clientIp,
            String responseCode,
            Boolean success,
            LocalDateTime startTime,
            LocalDateTime endTime) {
        LambdaQueryWrapper<OpenApiRequestLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(requestId)) {
            wrapper.eq(OpenApiRequestLog::getRequestId, requestId.trim());
        }
        if (StringUtils.hasText(appId)) {
            wrapper.eq(OpenApiRequestLog::getAppId, appId.trim());
        }
        if (StringUtils.hasText(apiCode)) {
            wrapper.eq(OpenApiRequestLog::getApiCode, apiCode.trim());
        }
        if (StringUtils.hasText(clientIp)) {
            wrapper.eq(OpenApiRequestLog::getClientIp, clientIp.trim());
        }
        if (StringUtils.hasText(responseCode)) {
            wrapper.eq(OpenApiRequestLog::getResponseCode, responseCode.trim());
        }
        if (success != null) {
            wrapper.eq(OpenApiRequestLog::getSuccess, success);
        }
        if (startTime != null) {
            wrapper.ge(OpenApiRequestLog::getRequestTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(OpenApiRequestLog::getRequestTime, endTime);
        }
        return wrapper;
    }

    private long percentile95(List<OpenApiRequestLog> logs) {
        List<Long> durations = logs.stream()
                .map(OpenApiRequestLog::getDurationMs)
                .filter(value -> value != null && value >= 0)
                .sorted()
                .collect(Collectors.toCollection(ArrayList::new));
        if (durations.isEmpty()) {
            return 0L;
        }
        int index = (int) Math.ceil(durations.size() * 0.95D) - 1;
        return durations.get(Math.max(0, Math.min(index, durations.size() - 1)));
    }

    private Map<String, Long> countTopApis(List<OpenApiRequestLog> logs) {
        Map<String, Long> grouped = logs.stream()
                .map(OpenApiRequestLog::getApiCode)
                .filter(StringUtils::hasText)
                .collect(Collectors.groupingBy(value -> value, Collectors.counting()));
        return sortCounts(grouped, 8);
    }

    private Map<String, Long> countErrorCodes(List<OpenApiRequestLog> logs) {
        Map<String, Long> grouped = logs.stream()
                .filter(item -> !Boolean.TRUE.equals(item.getSuccess()))
                .map(item -> StringUtils.hasText(item.getResponseCode())
                        ? item.getResponseCode()
                        : "UNKNOWN")
                .collect(Collectors.groupingBy(value -> value, Collectors.counting()));
        return sortCounts(grouped, 8);
    }

    private Map<String, Long> sortCounts(Map<String, Long> grouped, int limit) {
        return grouped.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(limit)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    private OpenApiApp requireApp(Long id) {
        OpenApiApp app = appMapper.selectById(id);
        if (app == null) {
            throw new OpenApiCallException("OPENAPI_40401", "应用不存在", 404);
        }
        return app;
    }

    private OpenApiGrant requireGrant(Long id) {
        OpenApiGrant grant = grantMapper.selectById(id);
        if (grant == null) {
            throw new OpenApiCallException("OPENAPI_40401", "授权不存在", 404);
        }
        return grant;
    }

    private String normalizeGrantStatus(String status) {
        String normalized = status.trim().toUpperCase();
        if (!List.of("ENABLED", "DISABLED", "REVOKED").contains(normalized)) {
            throw badRequest("授权状态只能是ENABLED、DISABLED或REVOKED");
        }
        return normalized;
    }

    private int normalizePositive(int value, int min, int max, String label) {
        if (value < min || value > max) {
            throw badRequest(label + "必须在" + min + "到" + max + "之间");
        }
        return value;
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private double round2(double value) {
        return Math.round(value * 100D) / 100D;
    }

    private OpenApiCallException badRequest(String message) {
        return new OpenApiCallException("OPENAPI_40001", message, 400);
    }

    public record UpdateAppRequest(
            String appName,
            LocalDateTime validFrom,
            LocalDateTime validTo,
            String ipWhitelist,
            Integer qpsLimit,
            Integer maxPageSize) {
    }

    public record RotateSecretResponse(
            Long id,
            String appId,
            String appKey,
            String appSecret,
            LocalDateTime rotatedAt) {
    }

    public record UpdateGrantRequest(
            String status,
            String dataPermissionJson,
            String fieldPermissionJson,
            LocalDateTime validFrom,
            LocalDateTime validTo) {
    }

    public record DashboardView(
            int hours,
            long total,
            long successCount,
            long failureCount,
            double successRate,
            long averageDurationMs,
            long p95DurationMs,
            Map<String, Long> topApis,
            Map<String, Long> errorCodes,
            boolean sampleTruncated) {
    }
}
