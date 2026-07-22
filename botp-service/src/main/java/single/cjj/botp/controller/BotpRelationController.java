package single.cjj.botp.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.botp.domain.BotpContracts.DocumentRelation;
import single.cjj.botp.relation.BotpRelationRepository;

import java.util.List;

@RestController
@RequestMapping("/botp/relations")
public class BotpRelationController {

    private final BotpRelationRepository repository;

    public BotpRelationController(BotpRelationRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public ApiResponse<List<DocumentRelation>> list(
            @RequestParam(value = "tenantId", defaultValue = "default") String tenantId,
            @RequestParam(value = "sourceDocumentId", required = false) String sourceDocumentId,
            @RequestParam(value = "targetDocumentId", required = false) String targetDocumentId,
            @RequestParam(value = "limit", defaultValue = "50") int limit
    ) {
        return ApiResponse.success(repository.find(tenantId, sourceDocumentId, targetDocumentId, limit));
    }
}
