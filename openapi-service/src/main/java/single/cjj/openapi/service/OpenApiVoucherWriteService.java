package single.cjj.openapi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import single.cjj.openapi.dto.VoucherWriteCreateRequest;
import single.cjj.openapi.dto.VoucherWriteDetailResponse;
import single.cjj.openapi.dto.VoucherWriteLineRequest;
import single.cjj.openapi.dto.VoucherWriteStatusResponse;
import single.cjj.openapi.entity.OpenApiWriteRequest;
import single.cjj.openapi.entity.OpenApiWriteRequestLine;
import single.cjj.openapi.entity.OpenApiWriteStatusLog;
import single.cjj.openapi.exception.OpenApiCallException;
import single.cjj.openapi.mapper.OpenApiWriteRequestLineMapper;
import single.cjj.openapi.mapper.OpenApiWriteRequestMapper;
import single.cjj.openapi.mapper.OpenApiWriteStatusLogMapper;
import single.cjj.openapi.security.OpenApiContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class OpenApiVoucherWriteService {

    private final OpenApiWriteRequestMapper requestMapper;
    private final OpenApiWriteRequestLineMapper lineMapper;
    private final OpenApiWriteStatusLogMapper statusLogMapper;
    private final OpenApiPermissionService permissionService;
    private final OpenApiWriteStateService stateService;

    public OpenApiVoucherWriteService(OpenApiWriteRequestMapper requestMapper,
                                      OpenApiWriteRequestLineMapper lineMapper,
                                      OpenApiWriteStatusLogMapper statusLogMapper,
                                      OpenApiPermissionService permissionService,
                                      OpenApiWriteStateService stateService) {
        this.requestMapper = requestMapper;
        this.lineMapper = lineMapper;
        this.statusLogMapper = statusLogMapper;
        this.permissionService = permissionService;
        this.stateService = stateService;
    }

    @Transactional
    public VoucherWriteStatusResponse accept(OpenApiContext context,
                                             VoucherWriteCreateRequest input,
                                             String requestBodyHash) {
        if (context == null) {
            throw new OpenApiCallException("OPENAPI_50001", "OpenAPI认证上下文缺失", 500);
        }
        if (!StringUtils.hasText(requestBodyHash)) {
            throw new OpenApiCallException("OPENAPI_50001", "请求体摘要缺失", 500);
        }
        OpenApiPermissionService.VoucherWritePermission permission =
                permissionService.resolveVoucherWritePermission(context.getApp(), context.getGrant());
        validateInput(input, permission);

        OpenApiWriteRequest existing = findByIdempotency(
                context.getApp().getId(), input.getIdempotencyKey().trim()
        );
        if (existing != null) {
            return resolveIdempotent(existing, requestBodyHash);
        }
        OpenApiWriteRequest externalExisting = findByExternalBizNo(
                context.getApp().getId(), input.getExternalBizNo().trim()
        );
        if (externalExisting != null) {
            throw new OpenApiCallException(
                    "OPENAPI_VOUCHER_40902", "外部业务单号已存在，请查询原写入任务", 409
            );
        }
        enforceDailyQuota(context.getApp().getId(), permission.dailyWriteQuota());

        LocalDateTime now = LocalDateTime.now();
        OpenApiWriteRequest request = new OpenApiWriteRequest();
        request.setRequestId("vwr_" + UUID.randomUUID().toString().replace("-", ""));
        request.setAppId(context.getApp().getId());
        request.setAppExternalId(context.getApp().getAppId());
        request.setTenantId(permission.tenantId());
        request.setExternalBizNo(input.getExternalBizNo().trim());
        request.setIdempotencyKey(input.getIdempotencyKey().trim());
        request.setRequestBodyHash(requestBodyHash);
        request.setOrganizationId(input.getOrganizationId().trim());
        request.setBookId(input.getBookId().trim());
        request.setVoucherDate(input.getVoucherDate());
        request.setSummary(input.getSummary().trim());
        request.setStatus(OpenApiWriteStateService.ACCEPTED);
        request.setRetryCount(0);
        request.setMaxRetry(5);
        request.setCreatedAt(now);
        request.setUpdatedAt(now);

        try {
            requestMapper.insert(request);
        } catch (DuplicateKeyException e) {
            OpenApiWriteRequest concurrent = findByIdempotency(
                    context.getApp().getId(), input.getIdempotencyKey().trim()
            );
            if (concurrent != null) {
                return resolveIdempotent(concurrent, requestBodyHash);
            }
            throw new OpenApiCallException(
                    "OPENAPI_VOUCHER_40902", "外部业务单号或幂等键已存在", 409
            );
        }

        int lineNo = 1;
        for (VoucherWriteLineRequest source : input.getLines()) {
            OpenApiWriteRequestLine line = new OpenApiWriteRequestLine();
            line.setWriteRequestId(request.getId());
            line.setLineNo(source.getLineNo() == null ? lineNo : source.getLineNo());
            line.setAccountCode(source.getAccountCode().trim());
            line.setSummary(StringUtils.hasText(source.getSummary())
                    ? source.getSummary().trim() : input.getSummary().trim());
            line.setDebitAmount(amount(source.getDebitAmount()));
            line.setCreditAmount(amount(source.getCreditAmount()));
            line.setCurrency(trimToNull(source.getCurrency()));
            line.setRate(source.getRate());
            line.setOriginalAmount(source.getOriginalAmount());
            line.setCashflowItem(trimToNull(source.getCashflowItem()));
            lineMapper.insert(line);
            lineNo++;
        }
        stateService.initialize(request);
        return VoucherWriteStatusResponse.from(request);
    }

    public VoucherWriteStatusResponse statusForApp(Long appId, String requestId) {
        OpenApiWriteRequest request = requestMapper.selectOne(new LambdaQueryWrapper<OpenApiWriteRequest>()
                .eq(OpenApiWriteRequest::getAppId, appId)
                .eq(OpenApiWriteRequest::getRequestId, requestId));
        return VoucherWriteStatusResponse.from(require(request));
    }

    public VoucherWriteStatusResponse statusForAppByExternalNo(Long appId, String externalBizNo) {
        OpenApiWriteRequest request = requestMapper.selectOne(new LambdaQueryWrapper<OpenApiWriteRequest>()
                .eq(OpenApiWriteRequest::getAppId, appId)
                .eq(OpenApiWriteRequest::getExternalBizNo, externalBizNo));
        return VoucherWriteStatusResponse.from(require(request));
    }

    public VoucherWriteDetailResponse detail(String requestId) {
        OpenApiWriteRequest request = requestMapper.selectOne(new LambdaQueryWrapper<OpenApiWriteRequest>()
                .eq(OpenApiWriteRequest::getRequestId, requestId));
        require(request);
        List<OpenApiWriteRequestLine> lines = lineMapper.selectList(
                new LambdaQueryWrapper<OpenApiWriteRequestLine>()
                        .eq(OpenApiWriteRequestLine::getWriteRequestId, request.getId())
                        .orderByAsc(OpenApiWriteRequestLine::getLineNo)
                        .orderByAsc(OpenApiWriteRequestLine::getId)
        );
        List<OpenApiWriteStatusLog> logs = statusLogMapper.selectList(
                new LambdaQueryWrapper<OpenApiWriteStatusLog>()
                        .eq(OpenApiWriteStatusLog::getWriteRequestId, request.getId())
                        .orderByAsc(OpenApiWriteStatusLog::getId)
        );
        return new VoucherWriteDetailResponse(VoucherWriteStatusResponse.from(request), lines, logs);
    }

    public List<OpenApiWriteRequestLine> lines(Long writeRequestId) {
        return lineMapper.selectList(new LambdaQueryWrapper<OpenApiWriteRequestLine>()
                .eq(OpenApiWriteRequestLine::getWriteRequestId, writeRequestId)
                .orderByAsc(OpenApiWriteRequestLine::getLineNo)
                .orderByAsc(OpenApiWriteRequestLine::getId));
    }

    private void validateInput(VoucherWriteCreateRequest input,
                               OpenApiPermissionService.VoucherWritePermission permission) {
        if (input == null) {
            throw new OpenApiCallException("OPENAPI_VOUCHER_40001", "请求体不能为空", 400);
        }
        requireText(input.getExternalBizNo(), "外部业务单号", 128);
        requireText(input.getIdempotencyKey(), "幂等键", 128);
        requireText(input.getOrganizationId(), "组织", 64);
        requireText(input.getBookId(), "账簿", 64);
        requireText(input.getSummary(), "凭证摘要", 500);
        if (input.getVoucherDate() == null) {
            throw new OpenApiCallException("OPENAPI_VOUCHER_40001", "凭证日期不能为空", 400);
        }
        if (!permission.allowsOrganization(input.getOrganizationId().trim())) {
            throw new OpenApiCallException("OPENAPI_VOUCHER_40301", "组织不在写入授权范围", 403);
        }
        if (!permission.allowsBook(input.getBookId().trim())) {
            throw new OpenApiCallException("OPENAPI_VOUCHER_40302", "账簿不在写入授权范围", 403);
        }
        if (input.getLines() == null || input.getLines().size() < 2) {
            throw new OpenApiCallException("OPENAPI_VOUCHER_40001", "凭证至少需要两行分录", 400);
        }
        if (input.getLines().size() > permission.maxLinesPerVoucher()) {
            throw new OpenApiCallException("OPENAPI_VOUCHER_40004", "凭证分录数量超过授权上限", 400);
        }

        BigDecimal debitTotal = BigDecimal.ZERO;
        BigDecimal creditTotal = BigDecimal.ZERO;
        Set<Integer> lineNumbers = new HashSet<>();
        int index = 1;
        for (VoucherWriteLineRequest line : input.getLines()) {
            if (line == null || !StringUtils.hasText(line.getAccountCode())) {
                throw new OpenApiCallException("OPENAPI_VOUCHER_40002", "第" + index + "行科目不能为空", 400);
            }
            BigDecimal debit = amount(line.getDebitAmount());
            BigDecimal credit = amount(line.getCreditAmount());
            if (debit.signum() < 0 || credit.signum() < 0) {
                throw new OpenApiCallException("OPENAPI_VOUCHER_40001", "第" + index + "行金额不能小于0", 400);
            }
            if ((debit.signum() == 0) == (credit.signum() == 0)) {
                throw new OpenApiCallException(
                        "OPENAPI_VOUCHER_40001", "第" + index + "行必须且只能填写借方或贷方金额", 400
                );
            }
            int lineNo = line.getLineNo() == null ? index : line.getLineNo();
            if (lineNo <= 0 || !lineNumbers.add(lineNo)) {
                throw new OpenApiCallException("OPENAPI_VOUCHER_40001", "凭证分录行号必须为正数且不能重复", 400);
            }
            debitTotal = debitTotal.add(debit);
            creditTotal = creditTotal.add(credit);
            index++;
        }
        if (debitTotal.compareTo(creditTotal) != 0) {
            throw new OpenApiCallException("OPENAPI_VOUCHER_40001", "凭证借贷金额不平衡", 400);
        }
    }

    private void enforceDailyQuota(Long appId, int quota) {
        LocalDateTime start = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime end = LocalDateTime.now().toLocalDate().atTime(LocalTime.MAX);
        Long count = requestMapper.selectCount(new LambdaQueryWrapper<OpenApiWriteRequest>()
                .eq(OpenApiWriteRequest::getAppId, appId)
                .ge(OpenApiWriteRequest::getCreatedAt, start)
                .le(OpenApiWriteRequest::getCreatedAt, end));
        if (count != null && count >= quota) {
            throw new OpenApiCallException("OPENAPI_42902", "应用当日凭证写入额度已用完", 429);
        }
    }

    private VoucherWriteStatusResponse resolveIdempotent(OpenApiWriteRequest existing, String bodyHash) {
        if (!bodyHash.equalsIgnoreCase(existing.getRequestBodyHash())) {
            throw new OpenApiCallException(
                    "OPENAPI_VOUCHER_40901", "相同幂等键对应的请求内容不一致", 409
            );
        }
        return VoucherWriteStatusResponse.from(existing);
    }

    private OpenApiWriteRequest findByIdempotency(Long appId, String idempotencyKey) {
        return requestMapper.selectOne(new LambdaQueryWrapper<OpenApiWriteRequest>()
                .eq(OpenApiWriteRequest::getAppId, appId)
                .eq(OpenApiWriteRequest::getIdempotencyKey, idempotencyKey));
    }

    private OpenApiWriteRequest findByExternalBizNo(Long appId, String externalBizNo) {
        return requestMapper.selectOne(new LambdaQueryWrapper<OpenApiWriteRequest>()
                .eq(OpenApiWriteRequest::getAppId, appId)
                .eq(OpenApiWriteRequest::getExternalBizNo, externalBizNo));
    }

    private OpenApiWriteRequest require(OpenApiWriteRequest request) {
        if (request == null) {
            throw new OpenApiCallException("OPENAPI_40401", "写入任务不存在", 404);
        }
        return request;
    }

    private void requireText(String value, String label, int maxLength) {
        if (!StringUtils.hasText(value)) {
            throw new OpenApiCallException("OPENAPI_VOUCHER_40001", label + "不能为空", 400);
        }
        if (value.trim().length() > maxLength) {
            throw new OpenApiCallException("OPENAPI_VOUCHER_40001", label + "长度超过限制", 400);
        }
    }

    private BigDecimal amount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
