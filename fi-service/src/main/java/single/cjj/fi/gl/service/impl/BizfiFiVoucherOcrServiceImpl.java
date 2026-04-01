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
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class BizfiFiVoucherOcrServiceImpl implements BizfiFiVoucherOcrService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Map<String, String> COMMON_REPLACEMENTS = Map.ofEntries(
            Map.entry("银行存炊", "银行存款"),
            Map.entry("银行存歀", "银行存款"),
            Map.entry("应收账教", "应收账款"),
            Map.entry("盅收账款", "应收账款"),
            Map.entry("支付B客户项目货数", "支付B客户项目货款"),
            Map.entry("支付D客户项目货教", "支付D客户项目货款"),
            Map.entry("制单", "制单"),
            Map.entry("记涨", "记账"),
            Map.entry("财务主餘", "财务主管")
    );

    private static final List<String> KNOWN_SUBJECTS = Arrays.asList("应收账款", "应付账款", "银行存款", "库存现金", "其他应收款", "其他应付款");

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
        String normalized = text
                .replace('　', ' ')
                .replace("凭証", "凭证")
                .replace("憑证", "凭证")
                .replace("明目科目", "明细科目")
                .replace("\r", "\n");
        for (Map.Entry<String, String> e : COMMON_REPLACEMENTS.entrySet()) {
            normalized = normalized.replace(e.getKey(), e.getValue());
        }
        normalized = normalized.replaceAll("[ \t]+", " ");
        normalized = Arrays.stream(normalized.split("\\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining("\n"));
        return normalized;
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
            if (s.startsWith("支付") && s.contains("项目")) {
                return s.replaceAll("[0-9]+\\.[0-9]{1,2}.*$", "").trim();
            }
        }
        for (String l : lines) {
            String s = l.trim();
            if (s.contains("摘要") && s.length() > 2) {
                return s.replace("摘要", "").replace(":", "").replace("：", "").trim();
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

        // 优先按凭证模板行抽取：支付X客户项目货款 + 科目 + 金额
        Pattern amountPattern = Pattern.compile("([0-9]+\\.[0-9]{1,2})");
        for (String raw : arr) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            if (!line.contains("支付") || !line.contains("项目")) continue;

            Matcher am = amountPattern.matcher(line);
            if (!am.find()) continue;
            double val;
            try {
                val = Double.parseDouble(am.group(1));
            } catch (Exception e) {
                continue;
            }

            String subject = KNOWN_SUBJECTS.stream().filter(line::contains).findFirst().orElse("应收账款");
            String detail = extractCounterparty(line);
            String rowSummary = line.replaceAll("[0-9]+\\.[0-9]{1,2}.*$", "").trim();
            if (rowSummary.length() > 32) rowSummary = rowSummary.substring(0, 32);

            Map<String, Object> item = new HashMap<>();
            item.put("faccountCode", inferAccountCode(subject));
            item.put("fsummary", rowSummary.isEmpty() ? summary : rowSummary);
            item.put("fdebitAmount", val);
            item.put("fcreditAmount", 0);
            item.put("faccountName", subject);
            item.put("fdetailName", detail);
            lines.add(item);
        }

        // 没抽到模板行时，退化为数字行抽取
        if (lines.isEmpty()) {
            for (String line : arr) {
                Matcher nm = amountPattern.matcher(line);
                if (!nm.find()) continue;
                double val;
                try {
                    val = Double.parseDouble(nm.group(1));
                } catch (Exception e) {
                    continue;
                }
                if (val <= 0) continue;
                Map<String, Object> item = new HashMap<>();
                item.put("faccountCode", "1122");
                item.put("fsummary", summary);
                item.put("fdebitAmount", val);
                item.put("fcreditAmount", 0);
                item.put("faccountName", "应收账款");
                lines.add(item);
            }
        }

        double debitTotal = lines.stream().mapToDouble(it -> ((Number) it.getOrDefault("fdebitAmount", 0)).doubleValue()).sum();
        if (!lines.isEmpty()) {
            Map<String, Object> credit = new HashMap<>();
            credit.put("faccountCode", "1002");
            credit.put("fsummary", summary);
            credit.put("fdebitAmount", 0);
            credit.put("fcreditAmount", round2(debitTotal > 0 ? debitTotal : amount));
            credit.put("faccountName", "银行存款");
            lines.add(credit);
        }

        if (lines.isEmpty()) {
            Map<String, Object> l1 = new HashMap<>();
            l1.put("faccountCode", "1122");
            l1.put("fsummary", summary);
            l1.put("fdebitAmount", amount);
            l1.put("fcreditAmount", 0);
            Map<String, Object> l2 = new HashMap<>();
            l2.put("faccountCode", "1002");
            l2.put("fsummary", summary);
            l2.put("fdebitAmount", 0);
            l2.put("fcreditAmount", amount);
            lines.add(l1);
            lines.add(l2);
        }
        return lines;
    }

    private String extractCounterparty(String line) {
        Matcher m = Pattern.compile("([A-Za-z])客户").matcher(line);
        if (m.find()) {
            return m.group(1).toUpperCase() + "客户";
        }
        return "";
    }

    private String inferAccountCode(String subject) {
        if ("应收账款".equals(subject)) return "1122";
        if ("应付账款".equals(subject)) return "2202";
        if ("银行存款".equals(subject)) return "1002";
        if ("库存现金".equals(subject)) return "1001";
        if ("其他应收款".equals(subject)) return "1221";
        if ("其他应付款".equals(subject)) return "2241";
        return "1122";
    }

    private double round2(double v) {
        return Math.round(v * 100.0d) / 100.0d;
    }
}
