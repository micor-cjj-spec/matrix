package single.cjj.bizfi.util;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class TextEncodingFixer {
    private static final Map<Character, Byte> WINDOWS_1252_BYTES = buildWindows1252Bytes();

    private TextEncodingFixer() {}

    public static String fix(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        if (!looksGarbled(value)) {
            return value;
        }
        try {
            byte[] bytes = toLikelyOriginalBytes(value);
            if (bytes == null) {
                return value;
            }
            String repaired = new String(bytes, StandardCharsets.UTF_8);
            return countCjk(repaired) > countCjk(value) ? repaired : value;
        } catch (Exception ignored) {
            return value;
        }
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
