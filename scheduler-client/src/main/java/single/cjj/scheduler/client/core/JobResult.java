package single.cjj.scheduler.client.core;

import java.util.LinkedHashMap;
import java.util.Map;

public record JobResult(boolean success,
                        String code,
                        String message,
                        Map<String, Object> data) {

    public static JobResult succeeded() {
        return new JobResult(true, "SUCCESS", "执行成功", Map.of());
    }

    public static JobResult success(Map<String, Object> data) {
        return new JobResult(true, "SUCCESS", "执行成功",
                data == null ? Map.of() : new LinkedHashMap<>(data));
    }

    public static JobResult failure(String code, String message) {
        return new JobResult(false,
                code == null || code.isBlank() ? "BUSINESS_FAILED" : code,
                message == null ? "执行失败" : message,
                Map.of());
    }
}
