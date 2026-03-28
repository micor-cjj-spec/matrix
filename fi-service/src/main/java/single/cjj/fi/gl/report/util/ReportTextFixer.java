package single.cjj.fi.gl.report.util;

import java.nio.charset.StandardCharsets;

public final class ReportTextFixer {
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

    private static boolean looksGarbled(String value) {
        return value.contains("è") || value.contains("å") || value.contains("ç") || value.contains("ä") || value.contains("é");
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
