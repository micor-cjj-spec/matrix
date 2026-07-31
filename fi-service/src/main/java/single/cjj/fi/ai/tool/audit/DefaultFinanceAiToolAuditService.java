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
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
public class DefaultFinanceAiToolAuditService implements FinanceAiToolAuditService {

    private static final Logger log = LoggerFactory.getLogger(DefaultFinanceAiToolAuditService.class);
    private static final Set<String> ALLOWED_STATUSES = Set.of("STARTED", "SUCCEEDED", "FAILED");

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
            execution.setFconversationid(request.conversationId());
            execution.setFmodelname(request.modelName());
            execution.setFmodeltraceid(request.modelTraceId());
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

    @Override
    public FinanceAiToolExecutionPageResponse query(FinanceAiToolExecutionQuery query) {
        FinanceAiToolExecutionQuery normalized = normalizeQuery(query);
        QueryWrapper<FinanceAiToolExecution> countWrapper = buildQueryWrapper(normalized);
        long total = Optional.ofNullable(mapper.selectCount(countWrapper)).orElse(0L);
        if (total == 0) {
            return FinanceAiToolExecutionPageResponse.of(
                    normalized.page(),
                    normalized.size(),
                    0,
                    List.of()
            );
        }

        long offset = (long) (normalized.page() - 1) * normalized.size();
        QueryWrapper<FinanceAiToolExecution> pageWrapper = buildQueryWrapper(normalized)
                .orderByDesc("fcreatetime")
                .orderByDesc("fid")
                .last("LIMIT " + normalized.size() + " OFFSET " + offset);
        List<FinanceAiToolExecution> records = mapper.selectList(pageWrapper);
        return FinanceAiToolExecutionPageResponse.of(
                normalized.page(),
                normalized.size(),
                total,
                records
        );
    }

    private QueryWrapper<FinanceAiToolExecution> buildQueryWrapper(FinanceAiToolExecutionQuery query) {
        QueryWrapper<FinanceAiToolExecution> wrapper = new QueryWrapper<>();
        if (query.userId() != null) {
            wrapper.eq("fuserid", query.userId());
        }
        if (query.organizationId() != null) {
            wrapper.eq("forganizationid", query.organizationId());
        }
        if (StringUtils.hasText(query.period())) {
            wrapper.eq("fperiod", query.period());
        }
        if (StringUtils.hasText(query.status())) {
            wrapper.eq("fstatus", query.status());
        }
        if (StringUtils.hasText(query.conversationId())) {
            wrapper.eq("fconversationid", query.conversationId());
        }
        if (StringUtils.hasText(query.modelTraceId())) {
            wrapper.eq("fmodeltraceid", query.modelTraceId());
        }
        if (query.createdFrom() != null) {
            wrapper.ge("fcreatetime", query.createdFrom());
        }
        if (query.createdTo() != null) {
            wrapper.le("fcreatetime", query.createdTo());
        }
        return wrapper;
    }

    private FinanceAiToolExecutionQuery normalizeQuery(FinanceAiToolExecutionQuery query) {
        FinanceAiToolExecutionQuery source = query == null
                ? new FinanceAiToolExecutionQuery(null, null, null, null, null, null, null, null, 1, 20)
                : query;
        int page = source.page() > 0 ? source.page() : 1;
        int size = source.size() > 0 ? Math.min(source.size(), 100) : 20;
        Long userId = normalizePositiveId(source.userId(), "userId");
        Long organizationId = normalizePositiveId(source.organizationId(), "organizationId");
        String status = normalizeStatus(source.status());
        String period = trimToNull(source.period());
        String conversationId = trimToNull(source.conversationId());
        String modelTraceId = trimToNull(source.modelTraceId());
        if (source.createdFrom() != null
                && source.createdTo() != null
                && source.createdFrom().isAfter(source.createdTo())) {
            throw new IllegalArgumentException("createdFrom 不能晚于 createdTo");
        }
        return new FinanceAiToolExecutionQuery(
                userId,
                organizationId,
                period,
                status,
                conversationId,
                modelTraceId,
                source.createdFrom(),
                source.createdTo(),
                page,
                size
        );
    }

    private Long normalizePositiveId(Long value, String field) {
        if (value == null) {
            return null;
        }
        if (value <= 0) {
            throw new IllegalArgumentException(field + " 必须为正数");
        }
        return value;
    }

    private String normalizeStatus(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        normalized = normalized.toUpperCase(Locale.ROOT);
        if (!ALLOWED_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("status 仅支持 STARTED、SUCCEEDED、FAILED");
        }
        return normalized;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
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
