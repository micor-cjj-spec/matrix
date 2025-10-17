package single.cjj.share.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.share.model.AttachmentPresignRequest;
import single.cjj.share.model.AttachmentPresignResponse;
import single.cjj.share.service.AttachmentService;
import single.cjj.share.service.TaskService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/shared-cloud")
public class AttachmentController {

    private final AttachmentService attachmentService;
    private final TaskService taskService;

    public AttachmentController(AttachmentService attachmentService, TaskService taskService) {
        this.attachmentService = attachmentService;
        this.taskService = taskService;
    }

    @PostMapping("/attachments:presign")
    public Map<String, Object> presign(@RequestBody @Valid AttachmentPresignRequest request) {
        AttachmentPresignResponse response = attachmentService.generatePresignedUrl(request);
        Map<String, Object> result = new HashMap<>();
        result.put("data", response);
        return result;
    }

    @DeleteMapping("/attachments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        attachmentService.deleteAttachment(id);
        taskService.removeAttachmentReferences(id);
    }
}
