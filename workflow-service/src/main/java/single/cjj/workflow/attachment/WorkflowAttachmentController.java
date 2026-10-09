package single.cjj.workflow.attachment;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.bizfi.entity.ApiResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/workflow/files")
public class WorkflowAttachmentController {

    private final WorkflowAttachmentService attachmentService;

    public WorkflowAttachmentController(WorkflowAttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @PostMapping("/upload-url")
    public ApiResponse<AttachmentContracts.UploadUrlResponse> createUploadUrl(
            @Valid @RequestBody AttachmentContracts.CreateUploadRequest request) {
        return ApiResponse.success(attachmentService.createUploadUrl(request));
    }

    @PutMapping(value = "/{fileId}/content", consumes = MediaType.ALL_VALUE)
    public ResponseEntity<Void> upload(
            @PathVariable("fileId") String fileId,
            @RequestParam("expires") long expires,
            @RequestParam("signature") String signature,
            HttpServletRequest request) throws IOException {
        attachmentService.upload(
                fileId, expires, signature, request.getContentType(), request.getContentLengthLong(),
                request.getInputStream()
        );
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{fileId}/content")
    public ResponseEntity<Resource> download(
            @PathVariable("fileId") String fileId,
            @RequestParam("expires") long expires,
            @RequestParam("signature") String signature) {
        WorkflowAttachmentService.DownloadObject object = attachmentService.download(fileId, expires, signature);
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(object.fileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(object.contentType()))
                .contentLength(object.fileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(object.resource());
    }

    @PostMapping("/{fileId}/confirm")
    public ApiResponse<AttachmentContracts.AttachmentResponse> confirm(
            @PathVariable("fileId") String fileId,
            @Valid @RequestBody AttachmentContracts.ConfirmUploadRequest request) {
        return ApiResponse.success(attachmentService.confirm(fileId, request));
    }

    @GetMapping("/instances/{instanceId}")
    public ApiResponse<List<AttachmentContracts.AttachmentResponse>> listByInstance(
            @PathVariable("instanceId") String instanceId) {
        return ApiResponse.success(attachmentService.listByInstance(instanceId));
    }

    @GetMapping("/business/{sourceSystem}/{businessType}/{businessId}")
    public ApiResponse<List<AttachmentContracts.AttachmentResponse>> listByBusiness(
            @PathVariable("sourceSystem") String sourceSystem,
            @PathVariable("businessType") String businessType,
            @PathVariable("businessId") String businessId,
            @RequestParam("tenantId") String tenantId) {
        return ApiResponse.success(attachmentService.listByBusiness(
                tenantId, sourceSystem, businessType, businessId));
    }

    @DeleteMapping("/relations/{relationId}")
    public ApiResponse<Void> deleteRelation(
            @PathVariable("relationId") String relationId,
            @RequestParam("operatorId") String operatorId) {
        attachmentService.deleteRelation(relationId, operatorId);
        return ApiResponse.success(null);
    }
}
