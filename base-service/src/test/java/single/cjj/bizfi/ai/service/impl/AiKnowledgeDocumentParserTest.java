package single.cjj.bizfi.ai.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import single.cjj.bizfi.ai.config.KnowledgeIngestionProperties;
import single.cjj.bizfi.exception.BizException;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiKnowledgeDocumentParserTest {

    private KnowledgeIngestionProperties properties;
    private AiKnowledgeDocumentParser parser;

    @BeforeEach
    void setUp() {
        properties = new KnowledgeIngestionProperties();
        properties.setEnabled(true);
        properties.setMaxFileSizeBytes(1024L * 1024L);
        properties.setMaxExtractedCharacters(10_000);
        parser = new AiKnowledgeDocumentParser(properties);
    }

    @Test
    void shouldParseMarkdownDocument() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "月结制度.md",
                "text/markdown",
                "# 月结制度\n\n结账前需要完成凭证过账和损益结转。".getBytes(StandardCharsets.UTF_8)
        );

        AiKnowledgeDocumentParser.ParsedDocument result = parser.parse(file);

        assertEquals("月结制度.md", result.fileName());
        assertEquals("月结制度", result.defaultTitle());
        assertTrue(result.content().contains("损益结转"));
        assertEquals(64, result.contentHash().length());
    }

    @Test
    void shouldRejectContentThatDoesNotMatchPdfExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "fake.pdf",
                "application/pdf",
                "not a real pdf".getBytes(StandardCharsets.UTF_8)
        );

        BizException failure = assertThrows(BizException.class, () -> parser.parse(file));

        assertTrue(failure.getMessage().contains("扩展名"));
    }

    @Test
    void shouldRejectOversizedFileBeforeParsing() {
        properties.setMaxFileSizeBytes(4L);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large.txt",
                "text/plain",
                "12345".getBytes(StandardCharsets.UTF_8)
        );

        assertThrows(BizException.class, () -> parser.parse(file));
    }

    @Test
    void shouldRejectWhenIngestionIsDisabled() {
        properties.setEnabled(false);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "policy.txt",
                "text/plain",
                "policy".getBytes(StandardCharsets.UTF_8)
        );

        assertThrows(BizException.class, () -> parser.parse(file));
    }
}
