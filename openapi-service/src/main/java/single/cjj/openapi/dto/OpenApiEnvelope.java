package single.cjj.openapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OpenApiEnvelope<T> {

    private String code;
    private String message;
    private String requestId;
    private T data;

    public static <T> OpenApiEnvelope<T> success(String requestId, T data) {
        return new OpenApiEnvelope<>("0", "success", requestId, data);
    }

    public static <T> OpenApiEnvelope<T> error(String code, String message, String requestId) {
        return new OpenApiEnvelope<>(code, message, requestId, null);
    }
}
