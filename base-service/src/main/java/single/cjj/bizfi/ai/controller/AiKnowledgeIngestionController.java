package single.cjj.bizfi.ai.controller;

import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import single.cjj.bizfi.ai.config.KnowledgeIngestionProperties;
import single.cjj.bizfi.ai.dto.AiKnowledgeDocRequest;
import single.cjj.bizfi.ai.dto.AiKnowledgeImportResponse;
import single.cjj.bizfi.ai.dto.AiKnowledgeIndexJobResponse;
import single.cjj.bizfi.ai.service.impl.AiKnowledgeDocumentParser;
import single.cjj.bizfi.ai.service.impl.AiKnowledgeIndexJobService;
import single.cjj.bizfi.ai.service.impl.AiKnowledgeIngestionService;
import single.cjj.bizfi.entity.ApiResponse;

import java.util.List;

@RestController
@RequestMapping("/ai/knowledge")
public class AiKnowledgeIngestionController {

    private final AiKnowledgeDocumentParser documentParser;
    private final AiKnowledgeIngestionService ingestionService;
    private final AiKnowledgeIndexJobService indexJobService;
    private final KnowledgeIngestionProperties properties;

    public AiKnowledgeIngestionController(
            AiKnowledgeDocumentParser documentParser,
            AiKnowledgeIngestionService ingestionService,
            AiKnowledgeIndexJobService indexJobService,
            KnowledgeIngestionProperties properties
    ) {
        this.documentParser = documentParser;
        this.ingestionService = ingestionService;
        this.indexJobService = indexJobService;
        this.properties = properties;
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<AiKnowledgeImportResponse> importDocument(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "kbId", defaultValue = "default") String kbId,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "version", required = false) String version,
            @RequestParam(value = "status", defaultValue = "ACTIVE") String status
    ) {
        AiKnowledgeDocumentParser.ParsedDocument parsed = documentParser.parse(file);
        AiKnowledgeDocRequest request = new AiKnowledgeDocRequest();
        request.setKbId(kbId);
        request.setTitle(StringUtils.hasText(title) ? title.trim() : parsed.defaultTitle());
        request.setCategory(StringUtils.hasText(category) ? category.trim() : "导入文档");
        request.setSourcePath("upload://" + parsed.fileName());
        request.setContent(parsed.content());
        request.setVersion(version);
        request.setStatus(status);
        return ApiResponse.success(ingestionService.createImportedDocument(request, parsed));
    }

    @GetMapping("/index-jobs")
    public ApiResponse<List<AiKnowledgeIndexJobResponse>> listJobs(
            @RequestParam(value = "docId", required = false) String docId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "limit", defaultValue = "50") Integer limit
    ) {
        return ApiResponse.success(indexJobService.listJobs(docId, status, limit));
    }

    @PostMapping("/index-jobs/{jobId}/retry")
    public ApiResponse<AiKnowledgeIndexJobResponse> retryJob(@PathVariable("jobId") String jobId) {
        return ApiResponse.success(indexJobService.retry(jobId));
    }

    @PostMapping("/docs/{docId}/index-jobs")
    public ApiResponse<AiKnowledgeIndexJobResponse> enqueueDocumentIndex(@PathVariable("docId") String docId) {
        return ApiResponse.success(indexJobService.createReindexJob(docId));
    }

    @GetMapping("/ingestion/config")
    public ApiResponse<IngestionConfigResponse> ingestionConfig() {
        return ApiResponse.success(new IngestionConfigResponse(
                Boolean.TRUE.equals(properties.getEnabled()),
                properties.getMaxFileSizeBytes(),
                properties.getMaxExtractedCharacters(),
                List.of("pdf", "doc", "docx", "txt", "md", "markdown")
        ));
    }

    public record IngestionConfigResponse(
            boolean enabled,
            Long maxFileSizeBytes,
            Integer maxExtractedCharacters,
            List<String> allowedExtensions
    ) {
    }
}
