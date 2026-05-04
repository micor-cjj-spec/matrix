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
import single.cjj.bizfi.entity.ApiResponse;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ai/knowledge")
public class AiKnowledgeController {

    @Autowired
    private AiKnowledgeManagementService knowledgeManagementService;

    @GetMapping("/docs")
    public ApiResponse<IPage<AiKnowledgeDocSummaryResponse>> listDocs(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "status", required = false) String status
    ) {
        return ApiResponse.success(knowledgeManagementService.listDocs(page, size, keyword, category, status));
    }

    @GetMapping("/docs/{docId}")
    public ApiResponse<AiKnowledgeDocDetailResponse> getDoc(@PathVariable("docId") String docId) {
        return ApiResponse.success(knowledgeManagementService.getDoc(docId));
    }

    @PostMapping("/docs")
    public ApiResponse<AiKnowledgeDocDetailResponse> createDoc(@RequestBody AiKnowledgeDocRequest request) {
        return ApiResponse.success(knowledgeManagementService.createDoc(request));
    }

    @PutMapping("/docs/{docId}")
    public ApiResponse<AiKnowledgeDocDetailResponse> updateDoc(
            @PathVariable("docId") String docId,
            @RequestBody AiKnowledgeDocRequest request
    ) {
        return ApiResponse.success(knowledgeManagementService.updateDoc(docId, request));
    }

    @DeleteMapping("/docs/{docId}")
    public ApiResponse<Boolean> deleteDoc(@PathVariable("docId") String docId) {
        return ApiResponse.success(knowledgeManagementService.deleteDoc(docId));
    }

    @PostMapping("/docs/{docId}/rebuild")
    public ApiResponse<Map<String, Object>> rebuildChunks(@PathVariable("docId") String docId) {
        int chunkCount = knowledgeManagementService.rebuildChunks(docId);
        return ApiResponse.success(Map.of("docId", docId, "chunkCount", chunkCount));
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
        return ApiResponse.success(knowledgeManagementService.retrieve(question, kbIds, topK));
    }
}
