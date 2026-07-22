package single.cjj.openapi.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.openapi.contract.OpenApiPageResponse;
import single.cjj.openapi.dto.VoucherWriteDetailResponse;
import single.cjj.openapi.dto.VoucherWriteStatusResponse;
import single.cjj.openapi.entity.OpenApiWriteRequest;
import single.cjj.openapi.mapper.OpenApiWriteRequestMapper;
import single.cjj.openapi.service.OpenApiVoucherWriteService;
import single.cjj.openapi.service.OpenApiWriteStateService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/openapi/admin/write-requests")
public class OpenApiWriteAdminController {

    private final OpenApiWriteRequestMapper requestMapper;
    private final OpenApiVoucherWriteService writeService;
    private final OpenApiWriteStateService stateService;

    public OpenApiWriteAdminController(OpenApiWriteRequestMapper requestMapper,
                                       OpenApiVoucherWriteService writeService,
                                       OpenApiWriteStateService stateService) {
        this.requestMapper = requestMapper;
        this.writeService = writeService;
        this.stateService = stateService;
    }

    @GetMapping
    public ApiResponse<OpenApiPageResponse<VoucherWriteStatusResponse>> list(
            @RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(value = "requestId", required = false) String requestId,
            @RequestParam(value = "appId", required = false) String appId,
            @RequestParam(value = "externalBizNo", required = false) String externalBizNo,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "startTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(value = "endTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        LambdaQueryWrapper<OpenApiWriteRequest> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(requestId)) {
            wrapper.like(OpenApiWriteRequest::getRequestId, requestId.trim());
        }
        if (StringUtils.hasText(appId)) {
            wrapper.eq(OpenApiWriteRequest::getAppExternalId, appId.trim());
        }
        if (StringUtils.hasText(externalBizNo)) {
            wrapper.like(OpenApiWriteRequest::getExternalBizNo, externalBizNo.trim());
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(OpenApiWriteRequest::getStatus, status.trim().toUpperCase());
        }
        if (startTime != null) {
            wrapper.ge(OpenApiWriteRequest::getCreatedAt, startTime);
        }
        if (endTime != null) {
            wrapper.le(OpenApiWriteRequest::getCreatedAt, endTime);
        }
        wrapper.orderByDesc(OpenApiWriteRequest::getId);

        int safePageNo = Math.max(1, pageNo);
        int safePageSize = Math.max(1, Math.min(pageSize, 200));
        IPage<OpenApiWriteRequest> page = requestMapper.selectPage(
                new Page<>(safePageNo, safePageSize), wrapper
        );
        List<VoucherWriteStatusResponse> items = page.getRecords().stream()
                .map(VoucherWriteStatusResponse::from)
                .toList();
        return ApiResponse.success(OpenApiPageResponse.of(
                page.getTotal(), safePageNo, safePageSize, items
        ));
    }

    @GetMapping("/{requestId}")
    public ApiResponse<VoucherWriteDetailResponse> detail(
            @PathVariable("requestId") String requestId) {
        return ApiResponse.success(writeService.detail(requestId));
    }

    @PostMapping("/{requestId}/retry")
    public ApiResponse<Boolean> retry(@PathVariable("requestId") String requestId) {
        return ApiResponse.success(stateService.manualRetry(requestId));
    }
}
