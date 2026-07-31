package single.cjj.fi.ai.tool.audit;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.fi.ai.tool.FinanceMonthEndCloseToolRequest;
import single.cjj.fi.ai.tool.FinanceMonthEndCloseToolResponse;

import java.time.LocalDateTime;
import java.util.Optional;

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
                    new UpdateWrapper<FinanceAiToolExecution>()
                            .eq("frequestid", request.requestId())
                            .set("ftoolname", toolName)
                            .set("fstatus", "SUCCEEDED")
                            .set("freadinessscore", response.readinessScore())
                            .set("fblockingcount", response.blockingCount())
                            .set("fwarningcount", response.warningCount())
                            .set("fclosestatus", response.closeStatus())
                            .set("fdurationms", normalizeDuration(durationMillis))
                            .set("fendtime", now)
                            .set("fmodifytime", now)
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
                    new UpdateWrapper<FinanceAiToolExecution>()
                            .eq("frequestid", request.requestId())
                            .set("ftoolname", toolName)
                            .set("fstatus", "FAILED")
                            .set("fdurationms", normalizeDuration(durationMillis))
                            .set("ferrorcode", errorCode(failure))
                            .set("ferrormessage", safeErrorMessage(failure))
                            .set("fendtime", now)
                            .set("fmodifytime", now)
            );
        } catch (RuntimeException auditFailure) {
            log.warn("Failed to persist AI tool failure audit, requestId={}", safeRequestId(request), auditFailure);
        }
    }

    @Override
    public Optional<FinanceAiToolExecution> findByRequestId(String requestId) {
        if (!StringUtils.hasText(requestId)) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.selectOne(
                new QueryWrapper<FinanceAiToolExecution>()
                        .eq("frequestid", requestId.trim())
                        .last("LIMIT 1")
        ));
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
