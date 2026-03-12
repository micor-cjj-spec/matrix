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

            String boundary = "----BizfiBoundary" + System.currentTimeMillis();
            URL url = new URL("https://api.ocr.space/parse/image");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("apikey", "helloworld");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            ByteArrayOutputStream body = new ByteArrayOutputStream();
            writeField(body, boundary, "language", "chs");
            writeField(body, boundary, "isTable", "true");
            writeFile(body, boundary, "file", filename, contentType, file.getBytes());
            body.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

            conn.getOutputStream().write(body.toByteArray());
            int code = conn.getResponseCode();
            InputStream in = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
            String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);

            JsonNode root = objectMapper.readTree(json);
            String parsedText = "";
            JsonNode parsedResults = root.path("ParsedResults");
            if (parsedResults.isArray() && !parsedResults.isEmpty()) {
                parsedText = parsedResults.get(0).path("ParsedText").asText("");
            }

            Map<String, Object> voucher = new HashMap<>();
            voucher.put("fdate", extractDate(parsedText));
            voucher.put("fsummary", extractSummary(parsedText));
            voucher.put("famount", extractAmount(parsedText));

            Map<String, Object> result = new HashMap<>();
            result.put("rawText", parsedText);
            result.put("voucher", voucher);
            result.put("lines", new ArrayList<>());
            result.put("engine", "ocr.space-demo");
            result.put("warning", "当前为OCR初版，分录建议人工确认后导入");
            return result;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("OCR解析失败: " + e.getMessage());
        }
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
}
