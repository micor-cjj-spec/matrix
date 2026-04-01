package single.cjj.fi.gl.report.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.fi.gl.report.service.BizfiFiAnalysisReportService;
import single.cjj.fi.gl.report.service.BizfiFiBalanceSheetService;
import single.cjj.fi.gl.report.service.BizfiFiProfitStatementService;
import single.cjj.fi.gl.report.vo.EnterpriseTaxResultVO;
import single.cjj.fi.gl.report.vo.EnterpriseTaxRowVO;
import single.cjj.fi.gl.report.vo.FinancialIndicatorResultVO;
import single.cjj.fi.gl.report.vo.FinancialIndicatorRowVO;
import single.cjj.fi.gl.report.vo.ReportCheckResultVO;
import single.cjj.fi.gl.report.vo.ReportQueryResultVO;
import single.cjj.fi.gl.report.vo.ReportRowVO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BizfiFiAnalysisReportServiceImpl implements BizfiFiAnalysisReportService {

    private static final String CNY = "CNY";

    @Autowired
    private BizfiFiBalanceSheetService balanceSheetService;

    @Autowired
    private BizfiFiProfitStatementService profitStatementService;

    @Override
    public FinancialIndicatorResultVO financialIndicators(Long orgId, String period, String currency, Long balanceTemplateId, Long profitTemplateId) {
        String targetPeriod = normalizePeriod(period);
        String targetCurrency = StringUtils.hasText(currency) ? currency : CNY;

        ReportQueryResultVO balanceSheet = balanceSheetService.query(orgId, targetPeriod, targetCurrency, balanceTemplateId, true);
        ReportQueryResultVO profitStatement = profitStatementService.query(orgId, firstMonthOfYear(targetPeriod), targetPeriod, targetCurrency, profitTemplateId, true);

        FinancialIndicatorResultVO result = new FinancialIndicatorResultVO();
        result.setOrgId(orgId);
        result.setPeriod(targetPeriod);
        result.setCurrency(targetCurrency);
        result.setRows(new ArrayList<>());
        result.setChecks(new ArrayList<>());
        result.setWarnings(mergeWarnings(balanceSheet, profitStatement));

        Map<String, ReportRowVO> bsRows = rowsByCode(balanceSheet);
        Map<String, ReportRowVO> plRows = rowsByCode(profitStatement);

        BigDecimal totalAssets = amount(bsRows, "BS_ASSET");
        BigDecimal totalLiabilities = amount(bsRows, "BS_LIABILITY");
        BigDecimal totalEquity = amount(bsRows, "BS_EQUITY");
        BigDecimal currentAssets = amount(bsRows, "BS_CURRENT_ASSET");
        BigDecimal currentLiabilities = amount(bsRows, "BS_CURRENT_LIABILITY");
        BigDecimal revenue = amount(plRows, "PL_REVENUE", "REVENUE");
        BigDecimal cost = amount(plRows, "PL_COST", "COST");
        BigDecimal netProfit = amount(plRows, "PL_NET_PROFIT", "NET_PROFIT");

        List<FinancialIndicatorRowVO> rows = new ArrayList<>();
        rows.add(new FinancialIndicatorRowVO(
                "CURRENT_RATIO",
                "流动比率",
                "偿债能力",
                "流动资产 / 流动负债",
                dividePercentFree(currentAssets, currentLiabilities),
                "倍",
                "衡量短期偿债能力，通常越高越稳健。"
        ));
        rows.add(new FinancialIndicatorRowVO(
                "ASSET_LIABILITY_RATIO",
                "资产负债率",
                "资本结构",
                "总负债 / 总资产",
                toPercent(dividePercentFree(totalLiabilities, totalAssets)),
                "%",
                "反映负债对资产的占用程度。"
        ));
        rows.add(new FinancialIndicatorRowVO(
                "EQUITY_RATIO",
                "权益比率",
                "资本结构",
                "所有者权益 / 总资产",
                toPercent(dividePercentFree(totalEquity, totalAssets)),
                "%",
                "反映资产中由权益资金支持的占比。"
        ));
        rows.add(new FinancialIndicatorRowVO(
                "GROSS_MARGIN",
                "毛利率",
                "盈利能力",
                "(营业收入 - 营业成本) / 营业收入",
                toPercent(dividePercentFree(revenue.subtract(cost), revenue)),
                "%",
                "用于衡量主营业务的直接盈利能力。"
        ));
        rows.add(new FinancialIndicatorRowVO(
                "NET_MARGIN",
                "净利率",
                "盈利能力",
                "净利润 / 营业收入",
                toPercent(dividePercentFree(netProfit, revenue)),
                "%",
                "反映收入最终转化为净利润的能力。"
        ));
        rows.add(new FinancialIndicatorRowVO(
                "ROA",
                "总资产收益率",
                "经营效率",
                "净利润 / 总资产",
                toPercent(dividePercentFree(netProfit, totalAssets)),
                "%",
                "衡量资产整体使用效率。"
        ));
        result.setRows(rows);

        result.getChecks().add(new ReportCheckResultVO(
                "ANALYSIS_READY",
                !rows.isEmpty(),
                rows.isEmpty() ? "财务指标尚未生成。" : "财务指标已基于资产负债表和利润表计算。"
        ));
        if (isZero(totalAssets)) {
            result.getWarnings().add("总资产为 0，部分比率指标无法计算。");
        }
        if (isZero(revenue)) {
            result.getWarnings().add("营业收入为 0，毛利率和净利率可能不可用。");
        }
        return result;
    }

    @Override
    public EnterpriseTaxResultVO enterpriseTax(Long orgId, String period, String currency, Long profitTemplateId) {
        String targetPeriod = normalizePeriod(period);
        String targetCurrency = StringUtils.hasText(currency) ? currency : CNY;

        ReportQueryResultVO profitStatement = profitStatementService.query(orgId, firstMonthOfYear(targetPeriod), targetPeriod, targetCurrency, profitTemplateId, true);
        Map<String, ReportRowVO> plRows = rowsByCode(profitStatement);
        BigDecimal revenue = amount(plRows, "PL_REVENUE", "REVENUE");
        BigDecimal netProfit = amount(plRows, "PL_NET_PROFIT", "NET_PROFIT");

        BigDecimal vatRate = new BigDecimal("0.06");
        BigDecimal surtaxRate = new BigDecimal("0.12");
        BigDecimal incomeTaxRate = new BigDecimal("0.25");

        BigDecimal vatBase = revenue.max(BigDecimal.ZERO);
        BigDecimal vatAmount = percentOf(vatBase, vatRate);
        BigDecimal surtaxBase = vatAmount;
        BigDecimal surtaxAmount = percentOf(surtaxBase, surtaxRate);
        BigDecimal incomeTaxBase = netProfit.max(BigDecimal.ZERO);
        BigDecimal incomeTaxAmount = percentOf(incomeTaxBase, incomeTaxRate);

        List<EnterpriseTaxRowVO> rows = new ArrayList<>();
        rows.add(new EnterpriseTaxRowVO("VAT_EST", "增值税预估", vatBase, vatRate, vatAmount, "按营业收入与 6% 税率估算，需结合税制和抵扣规则复核。"));
        rows.add(new EnterpriseTaxRowVO("SURTAX_EST", "附加税费预估", surtaxBase, surtaxRate, surtaxAmount, "按增值税估算额的 12% 测算。"));
        rows.add(new EnterpriseTaxRowVO("CIT_EST", "企业所得税预估", incomeTaxBase, incomeTaxRate, incomeTaxAmount, "按净利润正数部分与 25% 税率估算。"));

        EnterpriseTaxResultVO result = new EnterpriseTaxResultVO();
        result.setOrgId(orgId);
        result.setPeriod(targetPeriod);
        result.setCurrency(targetCurrency);
        result.setRevenueAmount(revenue);
        result.setNetProfitAmount(netProfit);
        result.setTotalTaxAmount(vatAmount.add(surtaxAmount).add(incomeTaxAmount));
        result.setRows(rows);
        result.setChecks(new ArrayList<>());
        result.setWarnings(new ArrayList<>(profitStatement.getWarnings() == null ? Collections.emptyList() : profitStatement.getWarnings()));
        result.getChecks().add(new ReportCheckResultVO(
                "TAX_ESTIMATE_READY",
                true,
                "企业纳税表当前为分析预估口径，正式申报前仍需结合税务规则和抵扣数据复核。"
        ));
        if (isZero(revenue)) {
            result.getWarnings().add("营业收入为 0，本期增值税与附加税费预估可能为空。");
        }
        if (netProfit.compareTo(BigDecimal.ZERO) <= 0) {
            result.getWarnings().add("净利润小于等于 0，企业所得税预估按 0 处理。");
        }
        return result;
    }

    private Map<String, ReportRowVO> rowsByCode(ReportQueryResultVO result) {
        if (result == null || result.getRows() == null) {
            return Collections.emptyMap();
        }
        return result.getRows().stream()
                .filter(Objects::nonNull)
                .filter(row -> StringUtils.hasText(row.getItemCode()))
                .collect(Collectors.toMap(row -> row.getItemCode().toUpperCase(Locale.ROOT), Function.identity(), (left, right) -> left));
    }

    private BigDecimal amount(Map<String, ReportRowVO> rows, String... codes) {
        for (String code : codes) {
            if (!StringUtils.hasText(code)) {
                continue;
            }
            ReportRowVO row = rows.get(code.toUpperCase(Locale.ROOT));
            if (row != null) {
                if (row.getAmount() != null) {
                    return row.getAmount();
                }
                if (row.getCurrentAmount() != null) {
                    return row.getCurrentAmount();
                }
            }
        }
        return BigDecimal.ZERO;
    }

    private List<String> mergeWarnings(ReportQueryResultVO left, ReportQueryResultVO right) {
        List<String> warnings = new ArrayList<>();
        if (left != null && left.getWarnings() != null) {
            warnings.addAll(left.getWarnings());
        }
        if (right != null && right.getWarnings() != null) {
            for (String warning : right.getWarnings()) {
                if (!warnings.contains(warning)) {
                    warnings.add(warning);
                }
            }
        }
        return warnings;
    }

    private String normalizePeriod(String period) {
        if (!StringUtils.hasText(period)) {
            return YearMonth.now().toString();
        }
        try {
            return YearMonth.parse(period).toString();
        } catch (DateTimeParseException ex) {
            return YearMonth.now().toString();
        }
    }

    private String firstMonthOfYear(String period) {
        YearMonth ym = YearMonth.parse(normalizePeriod(period));
        return YearMonth.of(ym.getYear(), 1).toString();
    }

    private BigDecimal dividePercentFree(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return safe(numerator).divide(denominator, 4, RoundingMode.HALF_UP);
    }

    private BigDecimal toPercent(BigDecimal value) {
        return safe(value).multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal percentOf(BigDecimal base, BigDecimal rate) {
        return safe(base).multiply(safe(rate)).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private boolean isZero(BigDecimal value) {
        return safe(value).compareTo(BigDecimal.ZERO) == 0;
    }
}
