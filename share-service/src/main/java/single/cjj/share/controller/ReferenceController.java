package single.cjj.share.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.share.service.ReferenceService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/shared-cloud")
public class ReferenceController {

    private final ReferenceService referenceService;

    public ReferenceController(ReferenceService referenceService) {
        this.referenceService = referenceService;
    }

    @GetMapping("/reference/priorities")
    public Map<String, Object> getPriorities() {
        Map<String, Object> response = new HashMap<>();
        response.put("data", referenceService.getPriorities());
        return response;
    }

    @GetMapping("/reference/status")
    public Map<String, Object> getStatuses(@RequestParam(value = "from", required = false) String from) {
        Map<String, Object> response = new HashMap<>();
        response.put("data", referenceService.getStatuses(from));
        return response;
    }

    @GetMapping("/reference/modules")
    public Map<String, Object> getModules(@RequestParam(value = "keyword", required = false) String keyword) {
        Map<String, Object> response = new HashMap<>();
        response.put("data", referenceService.getModules(keyword));
        return response;
    }

    @GetMapping("/users")
    public Map<String, Object> searchUsers(@RequestParam(value = "query", required = false) String query) {
        Map<String, Object> response = new HashMap<>();
        response.put("data", referenceService.searchUsers(query));
        return response;
    }
}
