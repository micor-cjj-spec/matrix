package single.cjj.bizfi.ai.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.ai.service.AiPromptDocService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Collectors;

@Service
public class FileSystemAiPromptDocService implements AiPromptDocService {

    private static final String PROMPT_ROOT = "docs/ai/prompts";

    @Override
    public String loadSystemPrompt() {
        Path root = Paths.get(PROMPT_ROOT);
        if (!Files.exists(root) || !Files.isDirectory(root)) {
            return defaultPrompt();
        }
        try (var paths = Files.walk(root)) {
            String merged = paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".md"))
                    .sorted(Comparator.comparing(path -> path.toString().replace('\\', '/')))
                    .map(this::readFileSilently)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.joining("\n\n"));
            return StringUtils.hasText(merged) ? merged : defaultPrompt();
        } catch (Exception ignored) {
            return defaultPrompt();
        }
    }

    private String readFileSilently(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8).trim();
        } catch (IOException ignored) {
            return "";
        }
    }

    private String defaultPrompt() {
        return "你是 BizFi 企业管理平台的 AI 助手。默认使用中文回答，优先依据业务知识回答，不要编造系统功能。";
    }
}
