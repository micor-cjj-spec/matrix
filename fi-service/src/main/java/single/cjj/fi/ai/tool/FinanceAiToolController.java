package single.cjj.fi.ai.tool;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import single.cjj.fi.ai.tool.audit.FinanceAiToolAuditService;
import single.cjj.fi.ai.tool.audit.FinanceAiToolExecutionPageResponse;
import single.cjj.fi.ai.tool.audit.FinanceAiToolExecutionQuery;
import single.cjj.fi.ai.tool.audit.FinanceAiToolExecutionResponse;
import single.cjj.fi.gl.service.BizfiFiPeriodProcessService;
import single.cjj.fi.gl.vo.MonthEndWorkbenchResultVO;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/internal/ai/tools")
public class FinanceAiToolController {

    private static final String MONTH_END_CLOSE_CHECK = "month-end-close-check";
    private static final Pattern PERIOD_PATTERN = Pattern.compile("\\d{4}-(0[1-9]|1[0-2])");

    private final FinanceAiToolTokenGuard tokenGuard;
    private final BizfiFiPeriodProcessService periodProcessService;
    private final FinanceMonthEndCloseToolMapper mapper;
    private final FinanceAiToolAuditService auditService;

    public FinanceAiToolController(
            FinanceAiToolTokenGuard tokenGuard,
            BizfiFiPeriodProcessService periodProcessService,
            FinanceMonthEndCloseToolMapper mapper,
            FinanceAiToolAuditService auditService
    ) {
        this.tokenGuard = tokenGuard;
        this.periodProcessService = periodProcessService;
        this.mapper = mapper;
        this.auditService = auditService;
    }

    @PostMapping("/month-end-close-check")
    public FinanceMonthEndCloseToolResponse monthEndCloseCheck(
            @RequestHeader(value = FinanceAiToolTokenGuard.HEADER_NAME, required = false) String internalToken,
            @Valid @RequestBody FinanceMonthEndCloseToolRequest request
    ) {
        tokenGuard.verify(internalToken);
        long startedAt = System.nanoTime();
        auditService.recordStarted(MONTH_END_CLOSE_CHECK, request);
        try {
            MonthEndWorkbenchResultVO result = periodProcessService.monthEndWorkbench(
                    request.organizationId(),
                    request.period()
            );
            FinanceMonthEndCloseToolResponse response = mapper.map(result);
            auditService.recordSucceeded(
                    MONTH_END_CLOSE_CHECK,
                    request,
                    response,
                    elapsedMillis(startedAt)
            );
            return response;
        } catch (RuntimeException failure) {
            auditService.recordFailed(
                    MONTH_END_CLOSE_CHECK,
                    request,
                    failure,
                    elapsedMillis(startedAt)
            );
            throw failure;
        }
    }

    @GetMapping("/executions/{requestId}")
    public FinanceAiToolExecutionResponse execution(
            @RequestHeader(value = FinanceAiToolTokenGuard.HEADER_NAME, required = false) String internalToken,
            @PathVariable("requestId") String requestId
    ) {
        tokenGuard.verify(internalToken);
        if (!StringUtils.hasText(requestId) || requestId.length() > 64) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "requestId 无效");
        }
        return auditService.findByRequestId(requestId)
                .map(FinanceAiToolExecutionResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "tool execution 不存在"));
    }

    @GetMapping("/executions")
    public FinanceAiToolExecutionPageResponse executions(
            @RequestHeader(value = FinanceAiToolTokenGuard.HEADER_NAME, required = false) String internalToken,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "organizationId", required = false) Long organizationId,
            @RequestParam(value = "period", required = false) String period,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "conversationId", required = false) String conversationId,
            @RequestParam(value = "modelTraceId", required = false) String modelTraceId,
            @RequestParam(value = "createdFrom", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdFrom,
            @RequestParam(value = "createdTo", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdTo,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        tokenGuard.verify(internalToken);
        validateQueryText(period, conversationId, modelTraceId);
        try {
            return auditService.query(new FinanceAiToolExecutionQuery(
                    userId,
                    organizationId,
                    trimToNull(period),
                    trimToNull(status),
                    trimToNull(conversationId),
                    trimToNull(modelTraceId),
                    createdFrom,
                    createdTo,
                    page,
                    size
            ));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    private void validateQueryText(String period, String conversationId, String modelTraceId) {
        if (StringUtils.hasText(period) && !PERIOD_PATTERN.matcher(period.trim()).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "period 格式应为 yyyy-MM");
        }
        if (StringUtils.hasText(conversationId) && conversationId.trim().length() > 64) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "conversationId 过长");
        }
        if (StringUtils.hasText(modelTraceId) && modelTraceId.trim().length() > 64) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "modelTraceId 过长");
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private long elapsedMillis(long startedAtNanos) {
        return TimeUnit.NANOSECONDS.toMillis(Math.max(0L, System.nanoTime() - startedAtNanos));
    }
}
