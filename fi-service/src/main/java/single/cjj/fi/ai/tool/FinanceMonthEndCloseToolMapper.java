package single.cjj.fi.ai.tool;

import org.springframework.stereotype.Component;
import single.cjj.fi.gl.vo.MonthEndCheckItemVO;
import single.cjj.fi.gl.vo.MonthEndWorkbenchResultVO;

import java.util.List;

@Component
public class FinanceMonthEndCloseToolMapper {

    private final FinanceAiToolProperties properties;

    public FinanceMonthEndCloseToolMapper(FinanceAiToolProperties properties) {
        this.properties = properties;
    }

    public FinanceMonthEndCloseToolResponse map(MonthEndWorkbenchResultVO source) {
        if (source == null) {
            throw new IllegalStateException("月结检查未返回结果");
        }
        List<FinanceMonthEndCloseToolResponse.CheckItem> checkItems = safe(source.getCheckItems()).stream()
                .filter(item -> item != null)
                .limit(positive(properties.getMaxCheckItems(), 20))
                .map(this::mapCheckItem)
                .toList();
        List<String> warnings = safe(source.getWarnings()).stream()
                .filter(value -> value != null && !value.isBlank())
                .limit(positive(properties.getMaxWarnings(), 20))
                .toList();

        return new FinanceMonthEndCloseToolResponse(
                source.getForg(),
                source.getPeriod(),
                source.getPeriodStatus(),
                source.getCloseStatus(),
                source.getReadinessScore(),
                source.getCanClose(),
                source.getTotalCheckCount(),
                source.getPassedCount(),
                source.getWarningCount(),
                source.getBlockingCount(),
                source.getPendingCount(),
                source.getPeriodVoucherCount(),
                source.getPostedVoucherCount(),
                source.getPendingVoucherCount(),
                source.getExceptionVoucherCount(),
                source.getCheckedAt() == null ? null : source.getCheckedAt().toString(),
                checkItems,
                warnings,
                true
        );
    }

    private FinanceMonthEndCloseToolResponse.CheckItem mapCheckItem(MonthEndCheckItemVO item) {
        return new FinanceMonthEndCloseToolResponse.CheckItem(
                item.getCode(),
                item.getName(),
                item.getCategory(),
                item.getStatus(),
                item.getSeverity(),
                item.getMessage(),
                item.getActionHint(),
                item.getRelatedCount(),
                item.getBlocking()
        );
    }

    private <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : values;
    }

    private long positive(Integer value, int fallback) {
        return value != null && value > 0 ? value.longValue() : fallback;
    }
}
