package single.cjj.fi.ai.tool;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record FinanceMonthEndCloseToolRequest(
        @NotNull @Positive Long requestedByUserId,
        @NotNull @Positive Long organizationId,
        @NotBlank @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])") String period,
        @NotBlank String requestId
) {
}
