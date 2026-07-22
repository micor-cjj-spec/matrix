package single.cjj.scheduler.client.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.Map;

public class JobContext {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() { };

    private final String executionNo;
    private final String traceId;
    private final String jobCode;
    private final String handlerCode;
    private final int attemptNo;
    private final Map<String, Object> parameters;
    private final JobProgressReporter progressReporter;

    public JobContext(String executionNo,
                      String traceId,
                      String jobCode,
                      String handlerCode,
                      int attemptNo,
                      String parameterJson,
                      ObjectMapper objectMapper) {
        this(executionNo, traceId, jobCode, handlerCode, attemptNo,
                parameterJson, objectMapper, JobProgressReporter.NOOP);
    }

    public JobContext(String executionNo,
                      String traceId,
                      String jobCode,
                      String handlerCode,
                      int attemptNo,
                      String parameterJson,
                      ObjectMapper objectMapper,
                      JobProgressReporter progressReporter) {
        this.executionNo = executionNo;
        this.traceId = traceId;
        this.jobCode = jobCode;
        this.handlerCode = handlerCode;
        this.attemptNo = attemptNo;
        this.parameters = parse(parameterJson, objectMapper);
        this.progressReporter = progressReporter == null ? JobProgressReporter.NOOP : progressReporter;
    }

    private Map<String, Object> parse(String json, ObjectMapper objectMapper) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception e) {
            throw new IllegalArgumentException("任务参数不是合法 JSON", e);
        }
    }

    public void reportProgress(int progress, String stage, String message) {
        if (progress < 0 || progress > 100) {
            throw new IllegalArgumentException("任务进度必须在0到100之间");
        }
        progressReporter.report(progress, trim(stage, 64), trim(message, 500));
    }

    public String getExecutionNo() {
        return executionNo;
    }

    public String getTraceId() {
        return traceId;
    }

    public String getJobCode() {
        return jobCode;
    }

    public String getHandlerCode() {
        return handlerCode;
    }

    public int getAttemptNo() {
        return attemptNo;
    }

    public Map<String, Object> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }

    public String getString(String key) {
        Object value = parameters.get(key);
        return value == null ? null : String.valueOf(value);
    }

    public String getRequiredString(String key) {
        String value = getString(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("任务参数缺少必填项: " + key);
        }
        return value.trim();
    }

    public Long getLong(String key) {
        Object value = parameters.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }

    public Integer getInteger(String key) {
        Object value = parameters.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.valueOf(String.valueOf(value));
    }

    private String trim(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
