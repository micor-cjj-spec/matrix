package single.cjj.bizfi.ai.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.bizfi.ai.dto.AiCitationResponse;
import single.cjj.bizfi.ai.dto.AiKnowledgeChunkResponse;
import single.cjj.bizfi.ai.dto.AiKnowledgeDocDetailResponse;
import single.cjj.bizfi.ai.dto.AiKnowledgeDocRequest;
import single.cjj.bizfi.ai.dto.AiKnowledgeDocSummaryResponse;
import single.cjj.bizfi.ai.dto.AiKnowledgeRetrieveRequest;
import single.cjj.bizfi.ai.service.AiKnowledgeManagementService;
import single.cjj.bizfi.ai.service.AiKnowledgeService;
import single.cjj.bizfi.ai.service.impl.AiKnowledgeVectorIndexService;
import single.cjj.bizfi.entity.ApiResponse;

import java.util.List;

@RestController
@RequestMapping("/ai/knowledge")
public class AiKnowledgeController {

    @Autowired
    private AiKnowledgeManagementService knowledgeManagementService;

    @Autowired
    private AiKnowledgeService knowledgeRetrievalService;

    @Autowired
    private AiKnowledgeVectorIndexService vectorIndexService;

    @GetMapping("/docs")
    public ApiResponse<IPage<AiKnowledgeDocSummaryResponse>> listDocs(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "kbId", required = false) String kbId
    ) {
        return ApiResponse.success(knowledgeManagementService.listDocs(page, size, keyword, category, status, kbId));
    }

    @GetMapping("/docs/{docId}")
    public ApiResponse<AiKnowledgeDocDetailResponse> getDoc(@PathVariable("docId") String docId) {
        return ApiResponse.success(knowledgeManagementService.getDoc(docId));
    }

    @PostMapping("/docs")
    public ApiResponse<AiKnowledgeDocDetailResponse> createDoc(@RequestBody AiKnowledgeDocRequest request) {
        AiKnowledgeDocDetailResponse result = knowledgeManagementService.createDoc(request);
        vectorIndexService.indexDocumentIfEnabled(result.getDocId());
        return ApiResponse.success(result);
    }

    @PutMapping("/docs/{docId}")
    public ApiResponse<AiKnowledgeDocDetailResponse> updateDoc(
            @PathVariable("docId") String docId,
            @RequestBody AiKnowledgeDocRequest request
    ) {
        AiKnowledgeDocDetailResponse result = knowledgeManagementService.updateDoc(docId, request);
        vectorIndexService.indexDocumentIfEnabled(result.getDocId());
        return ApiResponse.success(result);
    }

    @DeleteMapping("/docs/{docId}")
    public ApiResponse<Boolean> deleteDoc(@PathVariable("docId") String docId) {
        boolean deleted = knowledgeManagementService.deleteDoc(docId);
        if (deleted) {
            vectorIndexService.deleteDocument(docId);
        }
        return ApiResponse.success(deleted);
    }

    @PostMapping("/docs/{docId}/rebuild")
    public ApiResponse<RebuildResponse> rebuildChunks(@PathVariable("docId") String docId) {
        int chunkCount = knowledgeManagementService.rebuildChunks(docId);
        AiKnowledgeVectorIndexService.IndexResult indexResult = vectorIndexService.indexDocumentIfEnabled(docId);
        return ApiResponse.success(new RebuildResponse(docId, chunkCount, indexResult));
    }

    @PostMapping("/docs/{docId}/reindex-vector")
    public ApiResponse<AiKnowledgeVectorIndexService.IndexResult> reindexDocumentVector(
            @PathVariable("docId") String docId
    ) {
        return ApiResponse.success(vectorIndexService.reindexDocument(docId));
    }

    @PostMapping("/reindex")
    public ApiResponse<AiKnowledgeVectorIndexService.BulkIndexResult> reindexAll(
            @RequestParam(value = "onlyMissing", defaultValue = "true") boolean onlyMissing
    ) {
        return ApiResponse.success(vectorIndexService.reindexAll(onlyMissing));
    }

    @GetMapping("/docs/{docId}/chunks")
    public ApiResponse<List<AiKnowledgeChunkResponse>> listChunks(@PathVariable("docId") String docId) {
        return ApiResponse.success(knowledgeManagementService.listChunks(docId));
    }

    @GetMapping("/categories")
    public ApiResponse<List<String>> listCategories() {
        return ApiResponse.success(knowledgeManagementService.listCategories());
    }

    @PostMapping("/retrieve")
    public ApiResponse<List<AiCitationResponse>> retrieve(@RequestBody AiKnowledgeRetrieveRequest request) {
        String question = request == null ? null : request.getQuestion();
        List<String> kbIds = request == null ? null : request.getKbIds();
        Integer topK = request == null ? null : request.getTopK();
        return ApiResponse.success(knowledgeRetrievalService.retrieve(question, kbIds, topK));
    }

    public record RebuildResponse(
            String docId,
            int chunkCount,
            AiKnowledgeVectorIndexService.IndexResult vectorIndex
    ) {
    }
}
