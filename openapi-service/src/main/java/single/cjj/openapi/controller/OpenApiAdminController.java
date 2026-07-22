package single.cjj.openapi.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.openapi.entity.OpenApiApp;
import single.cjj.openapi.entity.OpenApiDefinition;
import single.cjj.openapi.entity.OpenApiGrant;
import single.cjj.openapi.exception.OpenApiCallException;
import single.cjj.openapi.mapper.OpenApiAppMapper;
import single.cjj.openapi.mapper.OpenApiDefinitionMapper;
import single.cjj.openapi.mapper.OpenApiGrantMapper;
import single.cjj.openapi.service.OpenApiSecretService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 开放平台内部管理接口。
 * 该路径不属于 /open-api/**，由现有 Gateway JWT 认证链保护。
 */
@RestController
@RequestMapping("/openapi/admin")
public class OpenApiAdminController {

    private static final String DEFAULT_VOUCHER_PERMISSION =
            "{\"allowedStatuses\":[\"POSTED\"],\"maxHistoryMonths\":24}";

    private final OpenApiAppMapper appMapper;
    private final OpenApiDefinitionMapper definitionMapper;
    private final OpenApiGrantMapper grantMapper;
    private final OpenApiSecretService secretService;

    public OpenApiAdminController(OpenApiAppMapper appMapper,
                                  OpenApiDefinitionMapper definitionMapper,
                                  OpenApiGrantMapper grantMapper,
                                  OpenApiSecretService secretService) {
        this.appMapper = appMapper;
        this.definitionMapper = definitionMapper;
        this.grantMapper = grantMapper;
        this.secretService = secretService;
    }

    @PostMapping("/apps")
    public ApiResponse<CreateAppResponse> createApp(@RequestBody CreateAppRequest request) {
        if (request == null || !StringUtils.hasText(request.appName())) {
            throw new OpenApiCallException("OPENAPI_40001", "应用名称不能为空", 400);
        }
        OpenApiSecretService.GeneratedCredential credential = secretService.generateCredential();
        LocalDateTime now = LocalDateTime.now();

        OpenApiApp app = new OpenApiApp();
        app.setAppId(credential.appId());
        app.setAppName(request.appName().trim());
        app.setAppKey(credential.appKey());
        app.setAppSecretCipher(credential.appSecretCipher());
        app.setTenantId(StringUtils.hasText(request.tenantId()) ? request.tenantId().trim() : "default");
        app.setStatus("ENABLED");
        app.setValidFrom(request.validFrom() == null ? now : request.validFrom());
        app.setValidTo(request.validTo());
        app.setIpWhitelist(request.ipWhitelist());
        app.setQpsLimit(normalizePositive(request.qpsLimit(), 10, 10000));
        app.setMaxPageSize(normalizePositive(request.maxPageSize(), 200, 500));
        app.setCreatedAt(now);
        app.setUpdatedAt(now);
        appMapper.insert(app);

        return ApiResponse.success(new CreateAppResponse(
                app.getId(),
                app.getAppId(),
                app.getAppName(),
                app.getAppKey(),
                credential.appSecret()
        ));
    }

    @GetMapping("/apps")
    public ApiResponse<List<AppView>> listApps() {
        List<AppView> apps = appMapper.selectList(
                        new LambdaQueryWrapper<OpenApiApp>().orderByDesc(OpenApiApp::getId))
                .stream()
                .map(AppView::from)
                .collect(Collectors.toList());
        return ApiResponse.success(apps);
    }

    @PutMapping("/apps/{id}/status")
    public ApiResponse<Boolean> updateAppStatus(
            @PathVariable("id") Long id,
            @RequestParam("status") String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase();
        if (!"ENABLED".equals(normalized) && !"DISABLED".equals(normalized)) {
            throw new OpenApiCallException("OPENAPI_40001", "应用状态只能是ENABLED或DISABLED", 400);
        }
        OpenApiApp app = appMapper.selectById(id);
        if (app == null) {
            throw new OpenApiCallException("OPENAPI_40401", "应用不存在", 404);
        }
        app.setStatus(normalized);
        app.setUpdatedAt(LocalDateTime.now());
        return ApiResponse.success(appMapper.updateById(app) > 0);
    }

    @GetMapping("/definitions")
    public ApiResponse<List<OpenApiDefinition>> listDefinitions() {
        return ApiResponse.success(definitionMapper.selectList(
                new LambdaQueryWrapper<OpenApiDefinition>()
                        .orderByAsc(OpenApiDefinition::getApiCode)
                        .orderByAsc(OpenApiDefinition::getApiVersion)
        ));
    }

    @PostMapping("/grants")
    public ApiResponse<OpenApiGrant> saveGrant(@RequestBody SaveGrantRequest request) {
        if (request == null || request.appId() == null || request.apiDefinitionId() == null) {
            throw new OpenApiCallException("OPENAPI_40001", "应用和API定义不能为空", 400);
        }
        if (appMapper.selectById(request.appId()) == null) {
            throw new OpenApiCallException("OPENAPI_40401", "应用不存在", 404);
        }
        if (definitionMapper.selectById(request.apiDefinitionId()) == null) {
            throw new OpenApiCallException("OPENAPI_40401", "API定义不存在", 404);
        }

        OpenApiGrant grant = grantMapper.selectOne(new LambdaQueryWrapper<OpenApiGrant>()
                .eq(OpenApiGrant::getAppId, request.appId())
                .eq(OpenApiGrant::getApiDefinitionId, request.apiDefinitionId()));
        LocalDateTime now = LocalDateTime.now();
        if (grant == null) {
            grant = new OpenApiGrant();
            grant.setAppId(request.appId());
            grant.setApiDefinitionId(request.apiDefinitionId());
            grant.setCreatedAt(now);
        }
        grant.setStatus(StringUtils.hasText(request.status()) ? request.status().trim().toUpperCase() : "ENABLED");
        grant.setDataPermissionJson(StringUtils.hasText(request.dataPermissionJson())
                ? request.dataPermissionJson()
                : DEFAULT_VOUCHER_PERMISSION);
        grant.setFieldPermissionJson(request.fieldPermissionJson());
        grant.setValidFrom(request.validFrom());
        grant.setValidTo(request.validTo());
        grant.setUpdatedAt(now);

        if (grant.getId() == null) {
            grantMapper.insert(grant);
        } else {
            grantMapper.updateById(grant);
        }
        return ApiResponse.success(grant);
    }

    @GetMapping("/grants")
    public ApiResponse<List<OpenApiGrant>> listGrants(@RequestParam("appId") Long appId) {
        return ApiResponse.success(grantMapper.selectList(new LambdaQueryWrapper<OpenApiGrant>()
                .eq(OpenApiGrant::getAppId, appId)
                .orderByAsc(OpenApiGrant::getApiDefinitionId)));
    }

    private int normalizePositive(Integer value, int defaultValue, int maxValue) {
        if (value == null || value <= 0) {
            return defaultValue;
        }
        return Math.min(value, maxValue);
    }

    public record CreateAppRequest(
            String appName,
            String tenantId,
            LocalDateTime validFrom,
            LocalDateTime validTo,
            String ipWhitelist,
            Integer qpsLimit,
            Integer maxPageSize) {
    }

    public record CreateAppResponse(
            Long id,
            String appId,
            String appName,
            String appKey,
            String appSecret) {
    }

    public record SaveGrantRequest(
            Long appId,
            Long apiDefinitionId,
            String status,
            String dataPermissionJson,
            String fieldPermissionJson,
            LocalDateTime validFrom,
            LocalDateTime validTo) {
    }

    public record AppView(
            Long id,
            String appId,
            String appName,
            String appKey,
            String tenantId,
            String status,
            LocalDateTime validFrom,
            LocalDateTime validTo,
            String ipWhitelist,
            Integer qpsLimit,
            Integer maxPageSize,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {

        private static AppView from(OpenApiApp app) {
            return new AppView(
                    app.getId(),
                    app.getAppId(),
                    app.getAppName(),
                    app.getAppKey(),
                    app.getTenantId(),
                    app.getStatus(),
                    app.getValidFrom(),
                    app.getValidTo(),
                    app.getIpWhitelist(),
                    app.getQpsLimit(),
                    app.getMaxPageSize(),
                    app.getCreatedAt(),
                    app.getUpdatedAt()
            );
        }
    }
}
