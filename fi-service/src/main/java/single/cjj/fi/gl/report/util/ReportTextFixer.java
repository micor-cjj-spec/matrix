package single.cjj.fi.gl.report.util;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class ReportTextFixer {
    private static final Map<String, String> TEMPLATE_NAMES = Map.of(
            "BS-STD", "资产负债表（标准版）",
            "PL-STD", "利润表（标准版）",
            "CF-STD", "现金流量表（标准版）"
    );

    private static final Map<String, String> ITEM_NAMES = Map.ofEntries(
            Map.entry("BS_ASSET", "资产"),
            Map.entry("BS_CASH", "货币资金"),
            Map.entry("BS_LIAB_EQ", "负债和所有者权益"),
            Map.entry("PL_REVENUE", "营业收入"),
            Map.entry("PL_COST", "营业成本"),
            Map.entry("PL_NET_PROFIT", "净利润"),
            Map.entry("CF_OPERATING", "经营活动现金流量净额"),
            Map.entry("CF_INVESTING", "投资活动现金流量净额"),
            Map.entry("CF_FINANCING", "筹资活动现金流量净额"),
            Map.entry("CF_NET_INCREASE", "现金及现金等价物净增加额")
    );

    private ReportTextFixer() {}

    public static String fix(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        if (!looksGarbled(value)) {
            return value;
        }
        try {
            String repaired = new String(value.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
            return looksBetter(repaired, value) ? repaired : value;
        } catch (Exception ignored) {
            return value;
        }
    }

    public static String fixTemplateName(String code, String name) {
        return TEMPLATE_NAMES.getOrDefault(code, fix(name));
    }

    public static String fixItemName(String code, String name) {
        return ITEM_NAMES.getOrDefault(code, fix(name));
    }

    private static boolean looksGarbled(String value) {
        return value.contains("è") || value.contains("å") || value.contains("ç") || value.contains("ä") || value.contains("é") || value.contains("�");
    }

    private static boolean looksBetter(String repaired, String original) {
        return repaired != null && !repaired.isEmpty() && countCjk(repaired) >= countCjk(original);
    }

    private static int countCjk(String value) {
        int count = 0;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c >= 0x4E00 && c <= 0x9FFF) {
                count++;
            }
        }
        return count;
    }
}
