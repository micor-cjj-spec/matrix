package single.cjj.fi.gl.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.bizfi.exception.BizException;
import single.cjj.fi.gl.entity.BizfiFiVoucher;
import single.cjj.fi.gl.entity.BizfiFiVoucherLine;
import single.cjj.fi.gl.service.BizfiFiVoucherService;
import single.cjj.openapi.contract.OpenApiPageResponse;
import single.cjj.openapi.contract.OpenVoucherLineResponse;
import single.cjj.openapi.contract.OpenVoucherResponse;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 仅供 openapi-service 调用的凭证只读适配器。
 * 不暴露任何新增、修改、审核、过账、冲销和删除能力。
 */
@RestController
@RequestMapping("/internal/openapi/v1/vouchers")
public class BizfiFiVoucherOpenApiController {

    private static final String ALLOWED_STATUS_HEADER = "X-OpenApi-Allowed-Statuses";
    private static final Set<String> DEFAULT_ALLOWED_STATUSES = Set.of("POSTED");

    private final BizfiFiVoucherService voucherService;

    public BizfiFiVoucherOpenApiController(BizfiFiVoucherService voucherService) {
        this.voucherService = voucherService;
    }

    @GetMapping
    public ApiResponse<OpenApiPageResponse<OpenVoucherResponse>> list(
            @RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(value = "voucherNumber", required = false) String voucherNumber,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestHeader(value = ALLOWED_STATUS_HEADER, required = false) String allowedStatusesHeader) {

        Set<String> allowedStatuses = parseAllowedStatuses(allowedStatusesHeader);
        String effectiveStatus = resolveEffectiveStatus(status, allowedStatuses);

        Map<String, Object> query = new HashMap<>();
        query.put("number", voucherNumber);
        query.put("status", effectiveStatus);
        query.put("startDate", startDate);
        query.put("endDate", endDate);

        int safePageNo = Math.max(pageNo, 1);
        int safePageSize = Math.max(1, Math.min(pageSize, 500));
        IPage<BizfiFiVoucher> page = voucherService.list(safePageNo, safePageSize, query);

        List<OpenVoucherResponse> items = page.getRecords().stream()
                .filter(voucher -> allowedStatuses.contains(voucher.getFstatus()))
                .map(this::toOpenVoucher)
                .collect(Collectors.toList());

        return ApiResponse.success(OpenApiPageResponse.of(
                page.getTotal(),
                safePageNo,
                safePageSize,
                items
        ));
    }

    @GetMapping("/{voucherId}")
    public ApiResponse<OpenVoucherResponse> detail(
            @PathVariable("voucherId") Long voucherId,
            @RequestHeader(value = ALLOWED_STATUS_HEADER, required = false) String allowedStatusesHeader) {
        BizfiFiVoucher voucher = requireAllowedVoucher(voucherId, parseAllowedStatuses(allowedStatusesHeader));
        return ApiResponse.success(toOpenVoucher(voucher));
    }

    @GetMapping("/{voucherId}/lines")
    public ApiResponse<List<OpenVoucherLineResponse>> lines(
            @PathVariable("voucherId") Long voucherId,
            @RequestHeader(value = ALLOWED_STATUS_HEADER, required = false) String allowedStatusesHeader) {
        requireAllowedVoucher(voucherId, parseAllowedStatuses(allowedStatusesHeader));
        List<BizfiFiVoucherLine> lines = voucherService.listLines(voucherId);
        if (lines == null || lines.isEmpty()) {
            return ApiResponse.success(Collections.emptyList());
        }
        return ApiResponse.success(lines.stream().map(this::toOpenLine).collect(Collectors.toList()));
    }

    private BizfiFiVoucher requireAllowedVoucher(Long voucherId, Set<String> allowedStatuses) {
        BizfiFiVoucher voucher = voucherService.get(voucherId);
        if (voucher == null) {
            throw new BizException("凭证不存在");
        }
        if (!allowedStatuses.contains(voucher.getFstatus())) {
            throw new BizException("凭证不在应用授权的数据范围内");
        }
        return voucher;
    }

    private String resolveEffectiveStatus(String requestedStatus, Set<String> allowedStatuses) {
        if (StringUtils.hasText(requestedStatus)) {
            String normalized = requestedStatus.trim().toUpperCase();
            if (!allowedStatuses.contains(normalized)) {
                throw new BizException("请求的凭证状态不在应用授权范围内");
            }
            return normalized;
        }
        if (allowedStatuses.size() == 1) {
            return allowedStatuses.iterator().next();
        }
        // 现有凭证服务只支持单状态查询。多状态授权时先不下推状态，结果仍会二次过滤。
        return null;
    }

    private Set<String> parseAllowedStatuses(String header) {
        if (!StringUtils.hasText(header)) {
            return DEFAULT_ALLOWED_STATUSES;
        }
        Set<String> result = Arrays.stream(header.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(String::toUpperCase)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return result.isEmpty() ? DEFAULT_ALLOWED_STATUSES : result;
    }

    private OpenVoucherResponse toOpenVoucher(BizfiFiVoucher voucher) {
        return new OpenVoucherResponse(
                String.valueOf(voucher.getFid()),
                voucher.getFnumber(),
                voucher.getFdate(),
                voucher.getFsummary(),
                voucher.getFamount(),
                voucher.getFstatus(),
                voucher.getFcreatedBy(),
                voucher.getFcreatedTime(),
                voucher.getFauditedBy(),
                voucher.getFauditedTime(),
                voucher.getFpostedBy(),
                voucher.getFpostedTime()
        );
    }

    private OpenVoucherLineResponse toOpenLine(BizfiFiVoucherLine line) {
        return new OpenVoucherLineResponse(
                String.valueOf(line.getFid()),
                line.getFlineNo(),
                line.getFaccountCode(),
                line.getFsummary(),
                line.getFdebitAmount(),
                line.getFcreditAmount(),
                line.getFcurrency(),
                line.getFrate(),
                line.getForiginalAmount(),
                line.getFcashflowItem()
        );
    }
}
