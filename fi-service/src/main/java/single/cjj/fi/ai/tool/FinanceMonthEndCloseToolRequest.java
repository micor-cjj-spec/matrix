package single.cjj.fi.ai.tool;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record FinanceMonthEndCloseToolRequest(
        @NotNull @Positive Long requestedByUserId,
        @NotNull @Positive Long organizationId,
        @NotBlank @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])") String period,
        @NotBlank @Size(max = 64) String requestId,
        @NotBlank @Size(max = 64) String conversationId,
        @NotBlank @Size(max = 128) String modelName,
        @NotBlank @Size(max = 64) String modelTraceId
) {
    public FinanceMonthEndCloseToolRequest(
            Long requestedByUserId,
            Long organizationId,
            String period,
            String requestId
    ) {
        this(requestedByUserId, organizationId, period, requestId, "legacy", "unknown", "unknown");
    }
}
