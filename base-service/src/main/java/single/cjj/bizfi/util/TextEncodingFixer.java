package single.cjj.bizfi.util;

import java.nio.charset.StandardCharsets;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

public final class TextEncodingFixer {
    private static final Charset GBK = Charset.forName("GBK");
    private static final Map<Character, Byte> WINDOWS_1252_BYTES = buildWindows1252Bytes();

    private TextEncodingFixer() {}

    public static String fix(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        if (!looksGarbled(value) && !looksGbkMojibake(value)) {
            return value;
        }

        String latinRepaired = repairLatinMojibake(value);
        if (isBetterLatinRepair(value, latinRepaired)) {
            return latinRepaired;
        }

        String gbkRepaired = repairGbkMojibake(value);
        if (isBetterGbkRepair(value, gbkRepaired)) {
            return gbkRepaired;
        }

        return value;
    }

    private static boolean looksGarbled(String value) {
        return value.contains("è")
                || value.contains("å")
                || value.contains("ç")
                || value.contains("ä")
                || value.contains("é")
                || value.contains("Ã")
                || value.contains("Â")
                || value.contains("€")
                || value.contains("œ")
                || value.contains("�");
    }

    private static boolean looksGbkMojibake(String value) {
        return countGbkMojibakeHints(value) > 0;
    }

    private static String repairLatinMojibake(String value) {
        try {
            byte[] bytes = toLikelyOriginalBytes(value);
            return bytes == null ? value : new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return value;
        }
    }

    private static String repairGbkMojibake(String value) {
        try {
            return new String(value.getBytes(GBK), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return value;
        }
    }

    private static boolean isBetterLatinRepair(String original, String repaired) {
        return repaired != null
                && !repaired.equals(original)
                && !repaired.contains("�")
                && countCjk(repaired) > countCjk(original);
    }

    private static boolean isBetterGbkRepair(String original, String repaired) {
        return repaired != null
                && !repaired.equals(original)
                && !repaired.contains("�")
                && countGbkMojibakeHints(repaired) < countGbkMojibakeHints(original)
                && countCjk(repaired) >= Math.max(1, countCjk(original) / 2);
    }

    private static int countGbkMojibakeHints(String value) {
        int count = 0;
        String[] hints = {
                "璐㈠姟", "绯荤粺", "宸ヤ綔", "鐭ヨ瘑", "鍑瘉", "浼佷笟", "骞冲彴", "搴旂敤",
                "寰呭姙", "鏈堢粨", "鍗忓悓", "瑙勫垝", "杩涘叆", "绠＄悊", "鎼滅储", "鍔╂墜", "閫€鍑"
        };
        for (String hint : hints) {
            if (value.contains(hint)) {
                count++;
            }
        }
        String chars = "璐㈠姟绯荤粺宸蹭笂绾鐭瘑鍑瘉浼骞冲彴搴旂敤寰呭姙鏈堢粨鍗忓悓瑙勫垝杩涘叆绠＄悊鎼滅储鍔╂墜閫€";
        for (int i = 0; i < value.length(); i++) {
            if (chars.indexOf(value.charAt(i)) >= 0) {
                count++;
            }
        }
        return count;
    }

    private static byte[] toLikelyOriginalBytes(String value) {
        byte[] bytes = new byte[value.length()];
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c <= 0xFF) {
                bytes[i] = (byte) c;
                continue;
            }
            Byte mapped = WINDOWS_1252_BYTES.get(c);
            if (mapped == null) {
                return null;
            }
            bytes[i] = mapped;
        }
        return bytes;
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

    private static Map<Character, Byte> buildWindows1252Bytes() {
        Map<Character, Byte> map = new HashMap<>();
        map.put('€', (byte) 0x80);
        map.put('‚', (byte) 0x82);
        map.put('ƒ', (byte) 0x83);
        map.put('„', (byte) 0x84);
        map.put('…', (byte) 0x85);
        map.put('†', (byte) 0x86);
        map.put('‡', (byte) 0x87);
        map.put('ˆ', (byte) 0x88);
        map.put('‰', (byte) 0x89);
        map.put('Š', (byte) 0x8A);
        map.put('‹', (byte) 0x8B);
        map.put('Œ', (byte) 0x8C);
        map.put('Ž', (byte) 0x8E);
        map.put('‘', (byte) 0x91);
        map.put('’', (byte) 0x92);
        map.put('“', (byte) 0x93);
        map.put('”', (byte) 0x94);
        map.put('•', (byte) 0x95);
        map.put('–', (byte) 0x96);
        map.put('—', (byte) 0x97);
        map.put('˜', (byte) 0x98);
        map.put('™', (byte) 0x99);
        map.put('š', (byte) 0x9A);
        map.put('›', (byte) 0x9B);
        map.put('œ', (byte) 0x9C);
        map.put('ž', (byte) 0x9E);
        map.put('Ÿ', (byte) 0x9F);
        return map;
    }
}
