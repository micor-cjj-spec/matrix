package single.cjj.matrix.ai.tool.finance;

import java.util.List;

public record FinanceMonthEndCloseResult(
        Long organizationId,
        String period,
        String periodStatus,
        String closeStatus,
        Integer readinessScore,
        Boolean canClose,
        Integer totalCheckCount,
        Integer passedCount,
        Integer warningCount,
        Integer blockingCount,
        Integer pendingCount,
        Integer periodVoucherCount,
        Integer postedVoucherCount,
        Integer pendingVoucherCount,
        Integer exceptionVoucherCount,
        String checkedAt,
        List<CheckItem> checkItems,
        List<String> warnings,
        Boolean readOnly
) {
    public record CheckItem(
            String code,
            String name,
            String category,
            String status,
            String severity,
            String message,
            String actionHint,
            Integer relatedCount,
            Boolean blocking
    ) {
    }
}
