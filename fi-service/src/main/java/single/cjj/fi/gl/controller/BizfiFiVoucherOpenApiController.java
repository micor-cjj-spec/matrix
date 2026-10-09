package single.cjj.fi.gl.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
import single.cjj.openapi.contract.OpenVoucherDraftCreateCommand;
import single.cjj.openapi.contract.OpenVoucherDraftCreateResult;
import single.cjj.openapi.contract.OpenVoucherDraftLineCommand;
import single.cjj.openapi.contract.OpenVoucherLineResponse;
import single.cjj.openapi.contract.OpenVoucherResponse;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 仅供 openapi-service 调用的凭证适配器。
 * 查询按租户、组织、账簿和状态在 SQL 中过滤；写入仅创建草稿。
 */
@RestController
@RequestMapping("/internal/openapi/v1/vouchers")
public class BizfiFiVoucherOpenApiController {

    private static final String TENANT_HEADER = "X-OpenApi-Tenant-Id";
    private static final String ALLOWED_STATUS_HEADER = "X-OpenApi-Allowed-Statuses";
    private static final String ALLOWED_ORG_HEADER = "X-OpenApi-Allowed-Organizations";
    private static final String ALLOWED_BOOK_HEADER = "X-OpenApi-Allowed-Books";
    private static final Set<String> DEFAULT_ALLOWED_STATUSES = Set.of("POSTED");
    private static final Set<String> WILDCARD_SCOPE = Set.of("*");

    private final BizfiFiVoucherService voucherService;

    public BizfiFiVoucherOpenApiController(BizfiFiVoucherService voucherService) {
        this.voucherService = voucherService;
    }

    @PostMapping("/drafts")
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<OpenVoucherDraftCreateResult> createDraft(
            @RequestBody OpenVoucherDraftCreateCommand command) {
        validateDraftCommand(command);
        BizfiFiVoucher existing = findBySourceRequest(command.getTenantId(), command.getSourceRequestId());
        if (existing != null) {
            return ApiResponse.success(toDraftResult(existing));
        }

        BizfiFiVoucher voucher = new BizfiFiVoucher();
        voucher.setTenantId(command.getTenantId().trim());
        voucher.setOrganizationId(command.getOrganizationId().trim());
        voucher.setBookId(command.getBookId().trim());
        voucher.setSourceRequestId(command.getSourceRequestId().trim());
        voucher.setFdate(command.getVoucherDate());
        voucher.setFsummary(command.getSummary().trim());
        voucher.setFamount(command.getLines().stream()
                .map(OpenVoucherDraftLineCommand::getDebitAmount)
                .map(this::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        voucher.setFcreatedBy(StringUtils.hasText(command.getCreatedBy())
                ? command.getCreatedBy().trim() : "openapi");

        BizfiFiVoucher saved;
        try {
            saved = voucherService.saveDraft(voucher);
        } catch (DuplicateKeyException e) {
            BizfiFiVoucher concurrent = findBySourceRequest(command.getTenantId(), command.getSourceRequestId());
            if (concurrent != null) {
                return ApiResponse.success(toDraftResult(concurrent));
            }
            throw e;
        }

        List<BizfiFiVoucherLine> lines = command.getLines().stream()
                .map(this::toVoucherLine)
                .collect(Collectors.toList());
        voucherService.saveLines(saved.getFid(), lines);
        return ApiResponse.success(toDraftResult(voucherService.get(saved.getFid())));
    }

    @GetMapping
    public ApiResponse<OpenApiPageResponse<OpenVoucherResponse>> list(
            @RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(value = "voucherNumber", required = false) String voucherNumber,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "organizationId", required = false) String organizationId,
            @RequestParam(value = "bookId", required = false) String bookId,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestHeader(TENANT_HEADER) String tenantId,
            @RequestHeader(value = ALLOWED_STATUS_HEADER, required = false) String allowedStatusesHeader,
            @RequestHeader(value = ALLOWED_ORG_HEADER, required = false) String allowedOrganizationsHeader,
            @RequestHeader(value = ALLOWED_BOOK_HEADER, required = false) String allowedBooksHeader) {

        Set<String> allowedStatuses = parseScope(allowedStatusesHeader, DEFAULT_ALLOWED_STATUSES, true);
        Set<String> allowedOrganizations = parseScope(allowedOrganizationsHeader, WILDCARD_SCOPE, false);
        Set<String> allowedBooks = parseScope(allowedBooksHeader, WILDCARD_SCOPE, false);
        String effectiveStatus = resolveRequestedValue(status, allowedStatuses, "凭证状态", true);
        String effectiveOrganization = resolveRequestedValue(
                organizationId, allowedOrganizations, "组织", false
        );
        String effectiveBook = resolveRequestedValue(bookId, allowedBooks, "账簿", false);

        LambdaQueryWrapper<BizfiFiVoucher> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BizfiFiVoucher::getTenantId, tenantId.trim());
        if (StringUtils.hasText(voucherNumber)) {
            wrapper.like(BizfiFiVoucher::getFnumber, voucherNumber.trim());
        }
        if (StringUtils.hasText(effectiveStatus)) {
            wrapper.eq(BizfiFiVoucher::getFstatus, effectiveStatus);
        } else {
            wrapper.in(BizfiFiVoucher::getFstatus, allowedStatuses);
        }
        applyOrganizationScope(wrapper, effectiveOrganization, allowedOrganizations);
        applyBookScope(wrapper, effectiveBook, allowedBooks);
        if (StringUtils.hasText(startDate)) {
            wrapper.ge(BizfiFiVoucher::getFdate, startDate);
        }
        if (StringUtils.hasText(endDate)) {
            wrapper.le(BizfiFiVoucher::getFdate, endDate);
        }
        wrapper.orderByDesc(BizfiFiVoucher::getFdate).orderByDesc(BizfiFiVoucher::getFid);

        int safePageNo = Math.max(pageNo, 1);
        int safePageSize = Math.max(1, Math.min(pageSize, 500));
        IPage<BizfiFiVoucher> page = voucherService.page(
                new Page<>(safePageNo, safePageSize), wrapper
        );
        List<OpenVoucherResponse> items = page.getRecords().stream()
                .map(this::toOpenVoucher)
                .collect(Collectors.toList());

        return ApiResponse.success(OpenApiPageResponse.of(
                page.getTotal(), safePageNo, safePageSize, items
        ));
    }

    @GetMapping("/{voucherId}")
    public ApiResponse<OpenVoucherResponse> detail(
            @PathVariable("voucherId") Long voucherId,
            @RequestHeader(TENANT_HEADER) String tenantId,
            @RequestHeader(value = ALLOWED_STATUS_HEADER, required = false) String allowedStatusesHeader,
            @RequestHeader(value = ALLOWED_ORG_HEADER, required = false) String allowedOrganizationsHeader,
            @RequestHeader(value = ALLOWED_BOOK_HEADER, required = false) String allowedBooksHeader) {
        BizfiFiVoucher voucher = requireAllowedVoucher(
                voucherId,
                tenantId,
                parseScope(allowedStatusesHeader, DEFAULT_ALLOWED_STATUSES, true),
                parseScope(allowedOrganizationsHeader, WILDCARD_SCOPE, false),
                parseScope(allowedBooksHeader, WILDCARD_SCOPE, false)
        );
        return ApiResponse.success(toOpenVoucher(voucher));
    }

    @GetMapping("/{voucherId}/lines")
    public ApiResponse<List<OpenVoucherLineResponse>> lines(
            @PathVariable("voucherId") Long voucherId,
            @RequestHeader(TENANT_HEADER) String tenantId,
            @RequestHeader(value = ALLOWED_STATUS_HEADER, required = false) String allowedStatusesHeader,
            @RequestHeader(value = ALLOWED_ORG_HEADER, required = false) String allowedOrganizationsHeader,
            @RequestHeader(value = ALLOWED_BOOK_HEADER, required = false) String allowedBooksHeader) {
        requireAllowedVoucher(
                voucherId,
                tenantId,
                parseScope(allowedStatusesHeader, DEFAULT_ALLOWED_STATUSES, true),
                parseScope(allowedOrganizationsHeader, WILDCARD_SCOPE, false),
                parseScope(allowedBooksHeader, WILDCARD_SCOPE, false)
        );
        List<BizfiFiVoucherLine> lines = voucherService.listLines(voucherId);
        if (lines == null || lines.isEmpty()) {
            return ApiResponse.success(Collections.emptyList());
        }
        return ApiResponse.success(lines.stream().map(this::toOpenLine).collect(Collectors.toList()));
    }

    private void validateDraftCommand(OpenVoucherDraftCreateCommand command) {
        if (command == null
                || !StringUtils.hasText(command.getSourceRequestId())
                || !StringUtils.hasText(command.getTenantId())
                || !StringUtils.hasText(command.getOrganizationId())
                || !StringUtils.hasText(command.getBookId())
                || command.getVoucherDate() == null
                || !StringUtils.hasText(command.getSummary())) {
            throw new BizException("凭证草稿写入参数不完整");
        }
        if (command.getLines() == null || command.getLines().size() < 2) {
            throw new BizException("凭证至少需要2条分录");
        }
        BigDecimal debit = BigDecimal.ZERO;
        BigDecimal credit = BigDecimal.ZERO;
        int index = 1;
        for (OpenVoucherDraftLineCommand line : command.getLines()) {
            if (line == null || !StringUtils.hasText(line.getAccountCode())) {
                throw new BizException("第" + index + "行科目不能为空");
            }
            BigDecimal lineDebit = amount(line.getDebitAmount());
            BigDecimal lineCredit = amount(line.getCreditAmount());
            if (lineDebit.signum() < 0 || lineCredit.signum() < 0
                    || (lineDebit.signum() == 0) == (lineCredit.signum() == 0)) {
                throw new BizException("第" + index + "行借贷金额不合法");
            }
            debit = debit.add(lineDebit);
            credit = credit.add(lineCredit);
            index++;
        }
        if (debit.compareTo(credit) != 0) {
            throw new BizException("凭证借贷金额不平衡");
        }
    }

    private BizfiFiVoucher findBySourceRequest(String tenantId, String sourceRequestId) {
        return voucherService.getOne(new LambdaQueryWrapper<BizfiFiVoucher>()
                .eq(BizfiFiVoucher::getTenantId, tenantId.trim())
                .eq(BizfiFiVoucher::getSourceRequestId, sourceRequestId.trim()), false);
    }

    private BizfiFiVoucherLine toVoucherLine(OpenVoucherDraftLineCommand source) {
        BizfiFiVoucherLine line = new BizfiFiVoucherLine();
        line.setFlineNo(source.getLineNo());
        line.setFaccountCode(source.getAccountCode());
        line.setFsummary(source.getSummary());
        line.setFdebitAmount(source.getDebitAmount());
        line.setFcreditAmount(source.getCreditAmount());
        line.setFcurrency(source.getCurrency());
        line.setFrate(source.getRate());
        line.setForiginalAmount(source.getOriginalAmount());
        line.setFcashflowItem(source.getCashflowItem());
        return line;
    }

    private OpenVoucherDraftCreateResult toDraftResult(BizfiFiVoucher voucher) {
        return new OpenVoucherDraftCreateResult(
                voucher.getFid(), voucher.getFnumber(), voucher.getFstatus()
        );
    }

    private BigDecimal amount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BizfiFiVoucher requireAllowedVoucher(Long voucherId,
                                                  String tenantId,
                                                  Set<String> statuses,
                                                  Set<String> organizations,
                                                  Set<String> books) {
        LambdaQueryWrapper<BizfiFiVoucher> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BizfiFiVoucher::getFid, voucherId)
                .eq(BizfiFiVoucher::getTenantId, tenantId.trim())
                .in(BizfiFiVoucher::getFstatus, statuses);
        if (!organizations.contains("*")) {
            wrapper.in(BizfiFiVoucher::getOrganizationId, organizations);
        }
        if (!books.contains("*")) {
            wrapper.in(BizfiFiVoucher::getBookId, books);
        }
        BizfiFiVoucher voucher = voucherService.getOne(wrapper, false);
        if (voucher == null) {
            throw new BizException("凭证不存在或不在应用授权的数据范围内");
        }
        return voucher;
    }

    private void applyOrganizationScope(LambdaQueryWrapper<BizfiFiVoucher> wrapper,
                                        String requested,
                                        Set<String> allowed) {
        if (StringUtils.hasText(requested)) {
            wrapper.eq(BizfiFiVoucher::getOrganizationId, requested);
        } else if (!allowed.contains("*")) {
            wrapper.in(BizfiFiVoucher::getOrganizationId, allowed);
        }
    }

    private void applyBookScope(LambdaQueryWrapper<BizfiFiVoucher> wrapper,
                                String requested,
                                Set<String> allowed) {
        if (StringUtils.hasText(requested)) {
            wrapper.eq(BizfiFiVoucher::getBookId, requested);
        } else if (!allowed.contains("*")) {
            wrapper.in(BizfiFiVoucher::getBookId, allowed);
        }
    }

    private String resolveRequestedValue(String requested,
                                         Set<String> allowed,
                                         String label,
                                         boolean uppercase) {
        if (!StringUtils.hasText(requested)) {
            return null;
        }
        String normalized = uppercase ? requested.trim().toUpperCase() : requested.trim();
        if (!allowed.contains("*") && !allowed.contains(normalized)) {
            throw new BizException("请求的" + label + "不在应用授权范围内");
        }
        return normalized;
    }

    private Set<String> parseScope(String header, Set<String> defaults, boolean uppercase) {
        if (!StringUtils.hasText(header)) {
            return defaults;
        }
        Set<String> result = Arrays.stream(header.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(value -> uppercase ? value.toUpperCase() : value)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (result.contains("*")) {
            return WILDCARD_SCOPE;
        }
        return result.isEmpty() ? defaults : result;
    }

    private OpenVoucherResponse toOpenVoucher(BizfiFiVoucher voucher) {
        String period = voucher.getFdate() == null
                ? null
                : String.format("%d-%02d", voucher.getFdate().getYear(), voucher.getFdate().getMonthValue());
        return new OpenVoucherResponse(
                String.valueOf(voucher.getFid()),
                voucher.getTenantId(),
                voucher.getOrganizationId(),
                voucher.getBookId(),
                voucher.getFnumber(),
                voucher.getFdate(),
                period,
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
