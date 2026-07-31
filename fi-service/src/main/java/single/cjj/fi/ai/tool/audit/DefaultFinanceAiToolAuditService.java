package single.cjj.fi.ai.tool.audit;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.fi.ai.tool.FinanceMonthEndCloseToolRequest;
import single.cjj.fi.ai.tool.FinanceMonthEndCloseToolResponse;

import java.time.LocalDateTime;

@Service
public class DefaultFinanceAiToolAuditService implements FinanceAiToolAuditService {

    private static final Logger log = LoggerFactory.getLogger(DefaultFinanceAiToolAuditService.class);

    private final FinanceAiToolExecutionMapper mapper;

    public DefaultFinanceAiToolAuditService(FinanceAiToolExecutionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void recordStarted(String toolName, FinanceMonthEndCloseToolRequest request) {
        try {
            LocalDateTime now = LocalDateTime.now();
            FinanceAiToolExecution execution = new FinanceAiToolExecution();
            execution.setFrequestid(request.requestId());
            execution.setFtoolname(toolName);
            execution.setFuserid(request.requestedByUserId());
            execution.setForganizationid(request.organizationId());
            execution.setFperiod(request.period());
            execution.setFstatus("STARTED");
            execution.setFstarttime(now);
            execution.setFcreatetime(now);
            execution.setFmodifytime(now);
            mapper.insert(execution);
        } catch (RuntimeException auditFailure) {
            log.warn("Failed to persist AI tool start audit, requestId={}", safeRequestId(request), auditFailure);
        }
    }

    @Override
    public void recordSucceeded(
            String toolName,
            FinanceMonthEndCloseToolRequest request,
            FinanceMonthEndCloseToolResponse response,
            long durationMillis
    ) {
        try {
            LocalDateTime now = LocalDateTime.now();
            mapper.update(
                    null,
                    new LambdaUpdateWrapper<FinanceAiToolExecution>()
                            .eq(FinanceAiToolExecution::getFrequestid, request.requestId())
                            .set(FinanceAiToolExecution::getFtoolname, toolName)
                            .set(FinanceAiToolExecution::getFstatus, "SUCCEEDED")
                            .set(FinanceAiToolExecution::getFreadinessscore, response.readinessScore())
                            .set(FinanceAiToolExecution::getFblockingcount, response.blockingCount())
                            .set(FinanceAiToolExecution::getFwarningcount, response.warningCount())
                            .set(FinanceAiToolExecution::getFclosestatus, response.closeStatus())
                            .set(FinanceAiToolExecution::getFdurationms, normalizeDuration(durationMillis))
                            .set(FinanceAiToolExecution::getFendtime, now)
                            .set(FinanceAiToolExecution::getFmodifytime, now)
            );
        } catch (RuntimeException auditFailure) {
            log.warn("Failed to persist AI tool success audit, requestId={}", safeRequestId(request), auditFailure);
        }
    }

    @Override
    public void recordFailed(
            String toolName,
            FinanceMonthEndCloseToolRequest request,
            Throwable failure,
            long durationMillis
    ) {
        try {
            LocalDateTime now = LocalDateTime.now();
            mapper.update(
                    null,
                    new LambdaUpdateWrapper<FinanceAiToolExecution>()
                            .eq(FinanceAiToolExecution::getFrequestid, request.requestId())
                            .set(FinanceAiToolExecution::getFtoolname, toolName)
                            .set(FinanceAiToolExecution::getFstatus, "FAILED")
                            .set(FinanceAiToolExecution::getFdurationms, normalizeDuration(durationMillis))
                            .set(FinanceAiToolExecution::getFerrorcode, errorCode(failure))
                            .set(FinanceAiToolExecution::getFerrormessage, safeErrorMessage(failure))
                            .set(FinanceAiToolExecution::getFendtime, now)
                            .set(FinanceAiToolExecution::getFmodifytime, now)
            );
        } catch (RuntimeException auditFailure) {
            log.warn("Failed to persist AI tool failure audit, requestId={}", safeRequestId(request), auditFailure);
        }
    }

    private long normalizeDuration(long value) {
        return Math.max(0L, value);
    }

    private String errorCode(Throwable failure) {
        if (failure == null) {
            return "UNKNOWN";
        }
        String name = failure.getClass().getSimpleName();
        return StringUtils.hasText(name) ? truncate(name, 64) : "UNKNOWN";
    }

    private String safeErrorMessage(Throwable failure) {
        String message = failure == null ? null : failure.getMessage();
        if (!StringUtils.hasText(message) && failure != null && failure.getCause() != null) {
            message = failure.getCause().getMessage();
        }
        if (!StringUtils.hasText(message)) {
            return "AI tool execution failed";
        }
        return truncate(message.replace('\r', ' ').replace('\n', ' '), 500);
    }

    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private String safeRequestId(FinanceMonthEndCloseToolRequest request) {
        return request == null || !StringUtils.hasText(request.requestId()) ? "unknown" : request.requestId();
    }
}
