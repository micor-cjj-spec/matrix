package single.cjj.bizfi.ai.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import single.cjj.bizfi.ai.dto.AiKnowledgeDocDetailResponse;
import single.cjj.bizfi.ai.dto.AiKnowledgeDocRequest;
import single.cjj.bizfi.ai.dto.AiKnowledgeImportResponse;
import single.cjj.bizfi.ai.dto.AiKnowledgeIndexJobResponse;
import single.cjj.bizfi.ai.service.AiKnowledgeManagementService;

@Service
public class AiKnowledgeIngestionService {

    private final AiKnowledgeManagementService knowledgeManagementService;
    private final AiKnowledgeIndexJobService indexJobService;

    public AiKnowledgeIngestionService(
            AiKnowledgeManagementService knowledgeManagementService,
            AiKnowledgeIndexJobService indexJobService
    ) {
        this.knowledgeManagementService = knowledgeManagementService;
        this.indexJobService = indexJobService;
    }

    @Transactional(rollbackFor = Exception.class)
    public AiKnowledgeImportResponse createImportedDocument(
            AiKnowledgeDocRequest request,
            AiKnowledgeDocumentParser.ParsedDocument parsed
    ) {
        AiKnowledgeDocDetailResponse document = knowledgeManagementService.createDoc(request);
        AiKnowledgeIndexJobResponse job = indexJobService.createJob(
                document.getKbId(),
                document.getDocId(),
                parsed.fileName(),
                parsed.mediaType(),
                parsed.fileSize(),
                parsed.contentHash()
        );
        return new AiKnowledgeImportResponse(document, job);
    }
}
