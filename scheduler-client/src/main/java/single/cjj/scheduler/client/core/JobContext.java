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

    public JobContext(String executionNo,
                      String traceId,
                      String jobCode,
                      String handlerCode,
                      int attemptNo,
                      String parameterJson,
                      ObjectMapper objectMapper) {
        this.executionNo = executionNo;
        this.traceId = traceId;
        this.jobCode = jobCode;
        this.handlerCode = handlerCode;
        this.attemptNo = attemptNo;
        this.parameters = parse(parameterJson, objectMapper);
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
}
