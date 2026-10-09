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
import single.cjj.bizfi.ai.dto.AiKnowledgeAccessResponse;
import single.cjj.bizfi.ai.dto.AiKnowledgeAclRequest;
import single.cjj.bizfi.ai.dto.AiKnowledgeAclResponse;
import single.cjj.bizfi.ai.dto.AiKnowledgeBaseRequest;
import single.cjj.bizfi.ai.dto.AiKnowledgeBaseResponse;
import single.cjj.bizfi.ai.service.AiKnowledgeBaseService;
import single.cjj.bizfi.ai.service.impl.AiKnowledgeAclService;
import single.cjj.bizfi.entity.ApiResponse;

import java.util.List;

@RestController
@RequestMapping("/ai/knowledge/bases")
public class AiKnowledgeBaseController {

    private final AiKnowledgeBaseService knowledgeBaseService;
    private final AiKnowledgeAclService aclService;

    public AiKnowledgeBaseController(
            AiKnowledgeBaseService knowledgeBaseService,
            AiKnowledgeAclService aclService
    ) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.aclService = aclService;
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

    @GetMapping("/{kbId}/access")
    public ApiResponse<AiKnowledgeAccessResponse> currentAccess(@PathVariable("kbId") String kbId) {
        return ApiResponse.success(aclService.currentAccess(kbId));
    }

    @GetMapping("/{kbId}/acl")
    public ApiResponse<List<AiKnowledgeAclResponse>> listAcl(@PathVariable("kbId") String kbId) {
        return ApiResponse.success(aclService.listEntries(kbId));
    }

    @PutMapping("/{kbId}/acl")
    public ApiResponse<AiKnowledgeAclResponse> grantAcl(
            @PathVariable("kbId") String kbId,
            @RequestBody AiKnowledgeAclRequest request
    ) {
        return ApiResponse.success(aclService.grant(kbId, request));
    }

    @DeleteMapping("/{kbId}/acl/{aclId}")
    public ApiResponse<Boolean> revokeAcl(
            @PathVariable("kbId") String kbId,
            @PathVariable("aclId") Long aclId
    ) {
        return ApiResponse.success(aclService.revoke(kbId, aclId));
    }
}
