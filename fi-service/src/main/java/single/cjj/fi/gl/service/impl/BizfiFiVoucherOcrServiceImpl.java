package single.cjj.fi.gl.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import single.cjj.bizfi.exception.BizException;
import single.cjj.fi.gl.service.BizfiFiVoucherOcrService;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class BizfiFiVoucherOcrServiceImpl implements BizfiFiVoucherOcrService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Map<String, Object> parseFile(MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                throw new BizException("上传文件不能为空");
            }
            String filename = Optional.ofNullable(file.getOriginalFilename()).orElse("voucher");
            String contentType = Optional.ofNullable(file.getContentType()).orElse("application/octet-stream");

            byte[] bytes = file.getBytes();
            String parsedText = requestOcr(filename, contentType, bytes, true);
            // 表格模式下中文质量不佳时，自动降级重试一次（非表格模式）
            if (isLikelyGarbled(parsedText)) {
                String retry = requestOcr(filename, contentType, bytes, false);
                if (!isLikelyGarbled(retry) || retry.length() > parsedText.length()) {
                    parsedText = retry;
                }
            }
            parsedText = normalizeOcrText(parsedText);

            Map<String, Object> voucher = new HashMap<>();
            voucher.put("fdate", extractDate(parsedText));
            voucher.put("fsummary", extractSummary(parsedText));
            voucher.put("famount", extractAmount(parsedText));

            List<Map<String, Object>> lines = extractLines(parsedText, ((Number) voucher.get("famount")).doubleValue(), String.valueOf(voucher.get("fsummary")));

            Map<String, Object> result = new HashMap<>();
            result.put("rawText", parsedText);
            result.put("voucher", voucher);
            result.put("lines", lines);
            result.put("engine", "ocr.space-demo");
            result.put("warning", "已启用中文增强识别与乱码重试，分录请人工核对后导入");
            return result;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("OCR解析失败: " + e.getMessage());
        }
    }

    private String requestOcr(String filename, String contentType, byte[] bytes, boolean isTable) throws Exception {
        String boundary = "----BizfiBoundary" + System.currentTimeMillis();
        URL url = new URL("https://api.ocr.space/parse/image");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);
        conn.setRequestProperty("apikey", "helloworld");
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        ByteArrayOutputStream body = new ByteArrayOutputStream();
        writeField(body, boundary, "language", "chs");
        writeField(body, boundary, "isTable", String.valueOf(isTable));
        writeField(body, boundary, "scale", "true");
        writeField(body, boundary, "detectOrientation", "true");
        writeField(body, boundary, "OCREngine", "2");
        writeFile(body, boundary, "file", filename, contentType, bytes);
        body.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

        conn.getOutputStream().write(body.toByteArray());
        int code = conn.getResponseCode();
        InputStream in = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
        String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);

        JsonNode root = objectMapper.readTree(json);
        JsonNode parsedResults = root.path("ParsedResults");
        if (parsedResults.isArray() && !parsedResults.isEmpty()) {
            return parsedResults.get(0).path("ParsedText").asText("");
        }
        return "";
    }

    private boolean isLikelyGarbled(String text) {
        if (text == null || text.isBlank()) return true;
        int total = 0;
        int chinese = 0;
        int weird = 0;
        for (char c : text.toCharArray()) {
            if (Character.isWhitespace(c)) continue;
            total++;
            Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
            if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                    || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                    || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS) {
                chinese++;
            } else if (!Character.isLetterOrDigit(c)
                    && "，。；：、？！￥¥.-_/()（）[]【】+*\"'".indexOf(c) < 0) {
                weird++;
            }
        }
        if (total == 0) return true;
        double chineseRate = chinese * 1.0 / total;
        double weirdRate = weird * 1.0 / total;
        return chineseRate < 0.08 || weirdRate > 0.22;
    }

    private String normalizeOcrText(String text) {
        if (text == null) return "";
        return text
                .replace('　', ' ')
                .replace("凭証", "凭证")
                .replace("憑证", "凭证")
                .replace("银行存歀", "银行存款")
                .replace("明目科目", "明细科目");
    }

    private void writeField(ByteArrayOutputStream out, String boundary, String name, String value) throws Exception {
        out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        out.write((value + "\r\n").getBytes(StandardCharsets.UTF_8));
    }

    private void writeFile(ByteArrayOutputStream out, String boundary, String name, String filename, String contentType, byte[] bytes) throws Exception {
        out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + filename + "\"\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Type: " + contentType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(bytes);
        out.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private String extractDate(String text) {
        Pattern p1 = Pattern.compile("(20\\d{2})[-年/.](\\d{1,2})[-月/.](\\d{1,2})");
        Matcher m = p1.matcher(text);
        if (m.find()) {
            return String.format("%s-%02d-%02d", m.group(1), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)));
        }
        return LocalDate.now().toString();
    }

    private String extractSummary(String text) {
        String[] lines = text.split("\\r?\\n");
        for (String l : lines) {
            String s = l.trim();
            if (s.contains("摘要") && s.length() > 2) {
                return s.replace("摘要", "").replace(":", "").replace("：", "").trim();
            }
        }
        for (String l : lines) {
            String s = l.trim();
            if (s.length() >= 4 && s.length() <= 40 && !s.contains("借") && !s.contains("贷")) {
                return s;
            }
        }
        return "OCR导入凭证";
    }

    private double extractAmount(String text) {
        Pattern p = Pattern.compile("([0-9]+\\.[0-9]{1,2})");
        Matcher m = p.matcher(text);
        double max = 0;
        while (m.find()) {
            try {
                max = Math.max(max, Double.parseDouble(m.group(1)));
            } catch (Exception ignore) {
            }
        }
        return max > 0 ? max : 1.0;
    }

    private List<Map<String, Object>> extractLines(String text, double amount, String summary) {
        List<Map<String, Object>> lines = new ArrayList<>();
        String[] arr = text.split("\\r?\\n");
        Pattern accountPattern = Pattern.compile("\\b(\\d{3,6})\\b");
        for (String line : arr) {
            Matcher am = accountPattern.matcher(line);
            if (!am.find()) continue;
            String acc = am.group(1);
            Matcher nm = Pattern.compile("([0-9]+\\.[0-9]{1,2})").matcher(line);
            double val = 0;
            while (nm.find()) {
                try {
                    val = Math.max(val, Double.parseDouble(nm.group(1)));
                } catch (Exception ignore) {}
            }
            if (val <= 0) continue;
            boolean debit = line.contains("借");
            Map<String, Object> item = new HashMap<>();
            item.put("faccountCode", acc);
            item.put("fsummary", summary);
            item.put("fdebitAmount", debit ? val : 0);
            item.put("fcreditAmount", debit ? 0 : val);
            lines.add(item);
        }

        if (lines.isEmpty()) {
            Map<String, Object> l1 = new HashMap<>();
            l1.put("faccountCode", "1002");
            l1.put("fsummary", summary);
            l1.put("fdebitAmount", amount);
            l1.put("fcreditAmount", 0);
            Map<String, Object> l2 = new HashMap<>();
            l2.put("faccountCode", "1001");
            l2.put("fsummary", summary);
            l2.put("fdebitAmount", 0);
            l2.put("fcreditAmount", amount);
            lines.add(l1);
            lines.add(l2);
        }
        return lines;
    }
}
