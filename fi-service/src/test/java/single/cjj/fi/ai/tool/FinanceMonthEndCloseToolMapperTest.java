package single.cjj.fi.ai.tool;

import org.junit.jupiter.api.Test;
import single.cjj.fi.gl.vo.MonthEndCheckItemVO;
import single.cjj.fi.gl.vo.MonthEndWorkbenchResultVO;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinanceMonthEndCloseToolMapperTest {

    @Test
    void shouldMapOnlySafeSummaryFields() {
        FinanceAiToolProperties properties = new FinanceAiToolProperties();
        properties.setMaxCheckItems(1);
        properties.setMaxWarnings(1);
        FinanceMonthEndCloseToolMapper mapper = new FinanceMonthEndCloseToolMapper(properties);

        MonthEndWorkbenchResultVO source = new MonthEndWorkbenchResultVO();
        source.setForg(10L);
        source.setPeriod("2026-07");
        source.setPeriodStatus("OPEN");
        source.setCloseStatus("BLOCKED");
        source.setReadinessScore(60);
        source.setCanClose(false);
        source.setTotalCheckCount(2);
        source.setPassedCount(0);
        source.setWarningCount(1);
        source.setBlockingCount(1);
        source.setPendingCount(0);
        source.setPeriodVoucherCount(12);
        source.setPostedVoucherCount(8);
        source.setPendingVoucherCount(4);
        source.setExceptionVoucherCount(0);
        source.setCheckedAt(LocalDateTime.of(2026, 7, 31, 9, 0));
        source.setCheckItems(List.of(
                new MonthEndCheckItemVO(
                        "VOUCHER_POSTING",
                        "凭证过账",
                        "VOUCHER",
                        "BLOCKED",
                        "HIGH",
                        "存在 4 张未过账凭证",
                        "完成审核和过账后重试",
                        "/ledger/voucher",
                        4,
                        true
                ),
                new MonthEndCheckItemVO(
                        "REPORT",
                        "财务报表",
                        "REPORT",
                        "WARNING",
                        "MEDIUM",
                        "报表需要复核",
                        "检查报表",
                        "/ledger/report",
                        1,
                        false
                )
        ));
        source.setWarnings(List.of("存在未过账凭证", "报表需要复核"));

        FinanceMonthEndCloseToolResponse result = mapper.map(source);

        assertEquals(10L, result.organizationId());
        assertEquals(1, result.checkItems().size());
        assertEquals("VOUCHER_POSTING", result.checkItems().get(0).code());
        assertEquals(1, result.warnings().size());
        assertTrue(result.readOnly());
    }
}
