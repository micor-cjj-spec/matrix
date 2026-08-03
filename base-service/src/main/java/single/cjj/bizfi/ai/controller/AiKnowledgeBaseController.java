package single.cjj.bizfi.ai.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.bizfi.ai.dto.AiKnowledgeBaseRequest;
import single.cjj.bizfi.ai.dto.AiKnowledgeBaseResponse;
import single.cjj.bizfi.ai.service.AiKnowledgeBaseService;
import single.cjj.bizfi.entity.ApiResponse;

import java.util.List;

@RestController
@RequestMapping("/ai/knowledge/bases")
public class AiKnowledgeBaseController {

    private final AiKnowledgeBaseService knowledgeBaseService;

    public AiKnowledgeBaseController(AiKnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    @GetMapping
    public ApiResponse<List<AiKnowledgeBaseResponse>> listBases(
            @RequestParam(value = "status", required = false) String status
    ) {
        return ApiResponse.success(knowledgeBaseService.listBases(status));
    }

    @PostMapping
    public ApiResponse<AiKnowledgeBaseResponse> createBase(@RequestBody AiKnowledgeBaseRequest request) {
        return ApiResponse.success(knowledgeBaseService.createBase(request));
    }

    @PutMapping("/{kbId}")
    public ApiResponse<AiKnowledgeBaseResponse> updateBase(
            @PathVariable("kbId") String kbId,
            @RequestBody AiKnowledgeBaseRequest request
    ) {
        return ApiResponse.success(knowledgeBaseService.updateBase(kbId, request));
    }

    @DeleteMapping("/{kbId}")
    public ApiResponse<Boolean> deleteBase(@PathVariable("kbId") String kbId) {
        return ApiResponse.success(knowledgeBaseService.deleteBase(kbId));
    }
}
