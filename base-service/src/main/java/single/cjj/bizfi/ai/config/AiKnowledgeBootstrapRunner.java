package single.cjj.bizfi.ai.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.ai.entity.BizfiAiKnowledgeChunk;
import single.cjj.bizfi.ai.entity.BizfiAiKnowledgeDoc;
import single.cjj.bizfi.ai.mapper.BizfiAiKnowledgeChunkMapper;
import single.cjj.bizfi.ai.mapper.BizfiAiKnowledgeDocMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class AiKnowledgeBootstrapRunner implements ApplicationRunner {

    private static final String DOC_ROOT = "docs/business";

    @Autowired
    private BizfiAiKnowledgeDocMapper knowledgeDocMapper;

    @Autowired
    private BizfiAiKnowledgeChunkMapper knowledgeChunkMapper;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Long count = knowledgeDocMapper.selectCount(new LambdaQueryWrapper<>());
        if (count != null && count > 0) {
            return;
        }
        Path root = Paths.get(DOC_ROOT);
        if (!Files.exists(root) || !Files.isDirectory(root)) {
            return;
        }
        try (var paths = Files.walk(root)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".md"))
                    .forEach(this::importMarkdownDocSilently);
        }
    }

    private void importMarkdownDocSilently(Path path) {
        try {
            importMarkdownDoc(path);
        } catch (Exception ignored) {
        }
    }

    private void importMarkdownDoc(Path path) throws IOException {
        String content = Files.readString(path, StandardCharsets.UTF_8);
        if (!StringUtils.hasText(content)) {
            return;
        }
        String docId = buildDocId(path);
        LocalDateTime now = LocalDateTime.now();

        BizfiAiKnowledgeDoc doc = new BizfiAiKnowledgeDoc();
        doc.setFdocid(docId);
        doc.setFtitle(extractTitle(path, content));
        doc.setFcategory(extractCategory(path));
        doc.setFsourcepath(path.toString().replace('\\', '/'));
        doc.setFcontent(content);
        doc.setFversion("v1");
        doc.setFstatus("ACTIVE");
        doc.setFcreatetime(now);
        doc.setFmodifytime(now);
        knowledgeDocMapper.insert(doc);

        List<String> chunks = splitMarkdown(content);
        AtomicInteger seq = new AtomicInteger(1);
        for (String chunkText : chunks) {
            if (!StringUtils.hasText(chunkText)) {
                continue;
            }
            BizfiAiKnowledgeChunk chunk = new BizfiAiKnowledgeChunk();
            chunk.setFdocid(docId);
            chunk.setFchunkid(docId + "_" + seq.get());
            chunk.setFseq(seq.getAndIncrement());
            chunk.setFcontent(chunkText.trim());
            chunk.setFkeywords(buildKeywords(chunkText));
            chunk.setFcreatetime(now);
            knowledgeChunkMapper.insert(chunk);
        }
    }

    private String buildDocId(Path path) {
        return path.toString().replace('\\', '/').replace('/', '_').replace('.', '_').toLowerCase(Locale.ROOT);
    }

    private String extractTitle(Path path, String content) {
        for (String line : content.split("\\R")) {
            if (line.startsWith("#")) {
                return line.replaceFirst("^#+", "").trim();
            }
        }
        return path.getFileName().toString();
    }

    private String extractCategory(Path path) {
        Path parent = path.getParent();
        if (parent == null) {
            return "general";
        }
        Path fileName = parent.getFileName();
        return fileName == null ? "general" : fileName.toString();
    }

    private List<String> splitMarkdown(String content) {
        List<String> chunks = new ArrayList<>();
        String[] parts = content.split("(?m)^##+\\s+");
        if (parts.length <= 1) {
            chunks.add(limitChunk(content));
            return chunks;
        }
        for (String part : parts) {
            if (StringUtils.hasText(part)) {
                chunks.add(limitChunk(part));
            }
        }
        return chunks;
    }

    private String limitChunk(String text) {
        String normalized = text.trim();
        if (normalized.length() <= 1200) {
            return normalized;
        }
        return normalized.substring(0, 1200);
    }

    private String buildKeywords(String text) {
        String normalized = text.replaceAll("[\\r\\n，。！？、,.!?;；:()（）\\[\\]{}<>]", " ");
        String[] parts = normalized.split("\\s+");
        List<String> keywords = new ArrayList<>();
        for (String part : parts) {
            String keyword = part.trim().toLowerCase(Locale.ROOT);
            if (keyword.length() >= 2 && keywords.stream().noneMatch(keyword::equals)) {
                keywords.add(keyword);
            }
            if (keywords.size() >= 30) {
                break;
            }
        }
        return String.join(",", keywords);
    }
}
