package single.cjj.workflow.attachment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

public final class AttachmentContracts {

    private AttachmentContracts() {
    }

    public record CreateUploadRequest(
            @NotBlank String tenantId,
            @NotBlank String sourceSystem,
            @NotBlank String businessType,
            @NotBlank String businessId,
            String instanceId,
            String taskId,
            @NotBlank String categoryCode,
            @NotBlank String fileName,
            @NotBlank String contentType,
            @NotNull @Positive Long fileSize,
            String sha256,
            @NotBlank String operatorId
    ) {
    }

    public record UploadUrlResponse(
            String fileId,
            String relationId,
            String method,
            String uploadUrl,
            LocalDateTime expiresAt,
            String uploadStatus
    ) {
    }

    public record ConfirmUploadRequest(
            @NotBlank String operatorId,
            String sha256
    ) {
    }

    public record AttachmentResponse(
            String relationId,
            String fileId,
            String tenantId,
            String sourceSystem,
            String businessType,
            String businessId,
            String instanceId,
            String taskId,
            String categoryCode,
            String originalName,
            String contentType,
            long fileSize,
            String sha256,
            String uploadStatus,
            String scanStatus,
            String downloadUrl,
            LocalDateTime createdAt,
            LocalDateTime uploadedAt
    ) {
    }
}
