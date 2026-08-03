package single.cjj.bizfi.ai.service.impl;

import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import single.cjj.bizfi.ai.config.KnowledgeIngestionProperties;
import single.cjj.bizfi.exception.BizException;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;

@Service
public class AiKnowledgeDocumentParser {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "doc", "docx", "txt", "md", "markdown");

    private final KnowledgeIngestionProperties properties;
    private final Tika tika = new Tika();

    public AiKnowledgeDocumentParser(KnowledgeIngestionProperties properties) {
        this.properties = properties;
    }

    public ParsedDocument parse(MultipartFile file) {
        if (!Boolean.TRUE.equals(properties.getEnabled())) {
            throw new BizException("知识文件导入功能未启用");
        }
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择需要导入的文件");
        }
        long maxFileSize = positive(properties.getMaxFileSizeBytes(), 10L * 1024L * 1024L);
        if (file.getSize() > maxFileSize) {
            throw new BizException("文件大小不能超过 " + readableMegabytes(maxFileSize) + " MB");
        }

        String fileName = sanitizeFileName(file.getOriginalFilename());
        String extension = normalizeExtension(StringUtils.getFilenameExtension(fileName));
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BizException("仅支持 PDF、DOC、DOCX、TXT 和 Markdown 文件");
        }

        try {
            byte[] content = file.getBytes();
            Metadata metadata = new Metadata();
            metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, fileName);
            int maxCharacters = positive(properties.getMaxExtractedCharacters(), 2_000_000);
            String extracted = tika.parseToString(new ByteArrayInputStream(content), metadata, maxCharacters);
            String mediaType = resolveMediaType(metadata, content, fileName);
            validateDetectedType(extension, mediaType);
            String normalizedText = normalizeText(extracted);
            if (!StringUtils.hasText(normalizedText)) {
                throw new BizException("文件中未提取到可用文本，扫描版PDF请先进行OCR");
            }
            return new ParsedDocument(
                    fileName,
                    defaultTitle(fileName),
                    mediaType,
                    content.length,
                    sha256(content),
                    normalizedText
            );
        } catch (BizException failure) {
            throw failure;
        } catch (Exception failure) {
            throw new BizException("文件解析失败，请确认文件未损坏或加密");
        }
    }

    private String sanitizeFileName(String originalFileName) {
        String cleaned = StringUtils.cleanPath(StringUtils.hasText(originalFileName) ? originalFileName : "document");
        if (cleaned.contains("..")) {
            throw new BizException("文件名不合法");
        }
        int slash = Math.max(cleaned.lastIndexOf('/'), cleaned.lastIndexOf('\\'));
        String fileName = slash >= 0 ? cleaned.substring(slash + 1) : cleaned;
        return StringUtils.hasText(fileName) ? limit(fileName.trim(), 255) : "document";
    }

    private String normalizeExtension(String extension) {
        return StringUtils.hasText(extension) ? extension.trim().toLowerCase(Locale.ROOT) : "";
    }

    private String resolveMediaType(Metadata metadata, byte[] content, String fileName) throws Exception {
        String detected = metadata.get("Content-Type");
        if (!StringUtils.hasText(detected)) {
            detected = tika.detect(content, fileName);
        }
        return StringUtils.hasText(detected) ? detected.trim().toLowerCase(Locale.ROOT) : "application/octet-stream";
    }

    private void validateDetectedType(String extension, String mediaType) {
        boolean valid = switch (extension) {
            case "pdf" -> mediaType.contains("pdf");
            case "doc" -> mediaType.contains("msword") || mediaType.contains("x-tika-msoffice");
            case "docx" -> mediaType.contains("wordprocessingml") || mediaType.contains("x-tika-ooxml");
            case "txt", "md", "markdown" -> mediaType.startsWith("text/")
                    || mediaType.contains("markdown")
                    || mediaType.equals("application/octet-stream");
            default -> false;
        };
        if (!valid) {
            throw new BizException("文件内容与扩展名不匹配");
        }
    }

    private String normalizeText(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        return text.replace('\u0000', ' ')
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[\\t\\x0B\\f ]+", " ")
                .replaceAll(" *\\n *", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private String defaultTitle(String fileName) {
        String title = StringUtils.stripFilenameExtension(fileName);
        return StringUtils.hasText(title) ? limit(title.trim(), 255) : "导入知识文档";
    }

    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException("JVM 不支持 SHA-256", failure);
        }
    }

    private long positive(Long value, long fallback) {
        return value != null && value > 0 ? value : fallback;
    }

    private int positive(Integer value, int fallback) {
        return value != null && value > 0 ? value : fallback;
    }

    private long readableMegabytes(long bytes) {
        return Math.max(1L, bytes / 1024L / 1024L);
    }

    private String limit(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    public record ParsedDocument(
            String fileName,
            String defaultTitle,
            String mediaType,
            long fileSize,
            String contentHash,
            String content
    ) {
    }
}
