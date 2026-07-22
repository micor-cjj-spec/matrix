package single.cjj.workflow.attachment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.exception.BizException;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class WorkflowAttachmentService {

    private final WorkflowAttachmentRepository repository;
    private final WorkflowUploadSigner signer;
    private final Map<String, WorkflowStorageProvider> providers;
    private final String baseUrl;
    private final long uploadTtlSeconds;
    private final long downloadTtlSeconds;
    private final long maximumBytes;

    public WorkflowAttachmentService(
            WorkflowAttachmentRepository repository,
            WorkflowUploadSigner signer,
            List<WorkflowStorageProvider> providers,
            @Value("${workflow.attachment.base-url:http://localhost:10006}") String baseUrl,
            @Value("${workflow.attachment.upload-ttl-seconds:900}") long uploadTtlSeconds,
            @Value("${workflow.attachment.download-ttl-seconds:300}") long downloadTtlSeconds,
            @Value("${workflow.attachment.max-file-size-bytes:52428800}") long maximumBytes) {
        this.repository = repository;
        this.signer = signer;
        this.providers = providers.stream().collect(Collectors.toUnmodifiableMap(
                WorkflowStorageProvider::providerKey, Function.identity()));
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.uploadTtlSeconds = uploadTtlSeconds;
        this.downloadTtlSeconds = downloadTtlSeconds;
        this.maximumBytes = maximumBytes;
    }

    @Transactional
    public AttachmentContracts.UploadUrlResponse createUploadUrl(
            AttachmentContracts.CreateUploadRequest request) {
        validateCreateRequest(request);
        String fileId = newId();
        String relationId = newId();
        String objectKey = buildObjectKey(request.tenantId(), fileId, request.fileName());
        LocalDateTime now = LocalDateTime.now();
        repository.insertFile(new WorkflowAttachmentRepository.FileRow(
                fileId, request.tenantId(), "LOCAL", "workflow-attachments", objectKey,
                request.fileName(), request.contentType(), request.fileSize(), 0,
                normalizeSha(request.sha256()), null, "PENDING", "PENDING",
                request.operatorId(), 0, now, null
        ));
        repository.insertRelation(new WorkflowAttachmentRepository.RelationRow(
                relationId, fileId, request.tenantId(), request.sourceSystem(),
                request.businessType(), request.businessId(), request.instanceId(), request.taskId(),
                request.categoryCode().trim().toUpperCase(Locale.ROOT), request.operatorId(), now
        ));
        long expires = Instant.now().getEpochSecond() + uploadTtlSeconds;
        return new AttachmentContracts.UploadUrlResponse(
                fileId, relationId, "PUT", signedUrl("UPLOAD", fileId, expires),
                LocalDateTime.ofInstant(Instant.ofEpochSecond(expires), ZoneOffset.systemDefault()),
                "PENDING"
        );
    }

    public void upload(String fileId,
                       long expires,
                       String signature,
                       String contentType,
                       long contentLength,
                       InputStream inputStream) {
        if (!signer.verify("UPLOAD", fileId, expires, signature)) {
            throw new BizException("上传地址无效或已经过期");
        }
        WorkflowAttachmentRepository.FileRow file = requireFile(fileId);
        if (!"PENDING".equals(file.uploadStatus())) {
            throw new BizException("文件已上传或当前状态不允许上传");
        }
        if (StringUtils.hasText(contentType) && !file.contentType().equalsIgnoreCase(contentType)) {
            throw new BizException("上传 Content-Type 与申请信息不一致");
        }
        if (contentLength > file.expectedSize() || contentLength > maximumBytes) {
            throw new BizException("上传文件超过申请大小");
        }
        WorkflowStorageProvider provider = requireProvider(file.storageProvider());
        try {
            WorkflowStorageProvider.StoredObject stored = provider.put(
                    file.objectKey(), inputStream, Math.min(file.expectedSize(), maximumBytes));
            if (stored.size() != file.expectedSize()) {
                provider.delete(file.objectKey());
                throw new BizException("上传文件大小与申请信息不一致");
            }
            if (StringUtils.hasText(file.expectedSha256())
                    && !file.expectedSha256().equalsIgnoreCase(stored.sha256())) {
                provider.delete(file.objectKey());
                throw new BizException("上传文件摘要校验失败");
            }
            if (repository.markStored(fileId, stored.size(), stored.sha256()) != 1) {
                provider.delete(file.objectKey());
                throw new BizException("文件状态已变化，请重新申请上传地址");
            }
        } catch (IOException ex) {
            throw new BizException("文件存储失败: " + ex.getMessage());
        }
    }

    @Transactional
    public AttachmentContracts.AttachmentResponse confirm(
            String fileId,
            AttachmentContracts.ConfirmUploadRequest request) {
        WorkflowAttachmentRepository.FileRow file = requireFile(fileId);
        if (!file.createdBy().equals(request.operatorId())) {
            throw new BizException("只有上传申请人可以确认文件");
        }
        if ("UPLOADED".equals(file.uploadStatus())) {
            return findAttachmentByFile(fileId);
        }
        if (!"STORED".equals(file.uploadStatus())) {
            throw new BizException("文件内容尚未上传完成");
        }
        String suppliedSha = normalizeSha(request.sha256());
        if (StringUtils.hasText(suppliedSha) && !suppliedSha.equalsIgnoreCase(file.sha256())) {
            throw new BizException("确认摘要与实际文件不一致");
        }
        if (repository.confirmUpload(fileId, file.version()) != 1) {
            throw new BizException("文件状态已变化，请刷新后重试");
        }
        return findAttachmentByFile(fileId);
    }

    public List<AttachmentContracts.AttachmentResponse> listByInstance(String instanceId) {
        return repository.listByInstance(instanceId).stream().map(this::toResponse).toList();
    }

    public List<AttachmentContracts.AttachmentResponse> listByBusiness(
            String tenantId, String sourceSystem, String businessType, String businessId) {
        return repository.listByBusiness(tenantId, sourceSystem, businessType, businessId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public void deleteRelation(String relationId, String operatorId) {
        WorkflowAttachmentRepository.RelationRow relation = repository.findRelation(relationId)
                .orElseThrow(() -> new BizException("影像关联不存在"));
        if (!relation.createdBy().equals(operatorId)) {
            throw new BizException("只有影像上传人可以删除关联");
        }
        if (repository.deactivateRelation(relationId, operatorId) != 1) {
            throw new BizException("影像关联已经删除");
        }
    }

    public DownloadObject download(String fileId, long expires, String signature) {
        if (!signer.verify("DOWNLOAD", fileId, expires, signature)) {
            throw new BizException("下载地址无效或已经过期");
        }
        WorkflowAttachmentRepository.FileRow file = requireFile(fileId);
        if (!"UPLOADED".equals(file.uploadStatus())) {
            throw new BizException("文件尚未确认上传");
        }
        try {
            return new DownloadObject(requireProvider(file.storageProvider()).load(file.objectKey()),
                    file.originalName(), file.contentType(), file.fileSize());
        } catch (IOException ex) {
            throw new BizException("读取文件失败: " + ex.getMessage());
        }
    }

    private AttachmentContracts.AttachmentResponse findAttachmentByFile(String fileId) {
        WorkflowAttachmentRepository.FileRow file = requireFile(fileId);
        WorkflowAttachmentRepository.RelationRow relation = repository.findRelationByFile(fileId)
                .orElseThrow(() -> new BizException("文件没有影像关联"));
        return toResponse(new WorkflowAttachmentRepository.AttachmentRow(
                relation.id(), file.id(), relation.tenantId(), relation.sourceSystem(),
                relation.businessType(), relation.businessId(), relation.instanceId(), relation.taskId(),
                relation.categoryCode(), file.originalName(), file.contentType(), file.fileSize(),
                file.sha256(), file.uploadStatus(), file.scanStatus(), file.objectKey(), file.createdBy(),
                relation.createdAt(), file.uploadedAt()
        ));
    }

    private AttachmentContracts.AttachmentResponse toResponse(WorkflowAttachmentRepository.AttachmentRow row) {
        long expires = Instant.now().getEpochSecond() + downloadTtlSeconds;
        return new AttachmentContracts.AttachmentResponse(
                row.relationId(), row.fileId(), row.tenantId(), row.sourceSystem(),
                row.businessType(), row.businessId(), row.instanceId(), row.taskId(), row.categoryCode(),
                row.originalName(), row.contentType(), row.fileSize(), row.sha256(), row.uploadStatus(),
                row.scanStatus(), signedUrl("DOWNLOAD", row.fileId(), expires),
                row.createdAt(), row.uploadedAt()
        );
    }

    private void validateCreateRequest(AttachmentContracts.CreateUploadRequest request) {
        if (request.fileSize() > maximumBytes) {
            throw new BizException("单个影像不能超过 " + maximumBytes + " 字节");
        }
        String type = request.contentType().toLowerCase(Locale.ROOT);
        if (!(type.startsWith("image/") || "application/pdf".equals(type))) {
            throw new BizException("只允许上传图片或 PDF 文件");
        }
        if (request.fileName().contains("/") || request.fileName().contains("\\")) {
            throw new BizException("文件名不能包含路径字符");
        }
    }

    private String signedUrl(String action, String fileId, long expires) {
        return baseUrl + "/api/workflow/files/" + fileId + "/content?expires=" + expires
                + "&signature=" + signer.sign(action, fileId, expires);
    }

    private String buildObjectKey(String tenantId, String fileId, String originalName) {
        String extension = "";
        int dot = originalName.lastIndexOf('.');
        if (dot >= 0 && dot < originalName.length() - 1) {
            extension = originalName.substring(dot).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9.]", "");
        }
        LocalDate date = LocalDate.now();
        return tenantId.replaceAll("[^a-zA-Z0-9_-]", "_") + "/"
                + date.getYear() + "/" + String.format("%02d", date.getMonthValue()) + "/"
                + String.format("%02d", date.getDayOfMonth()) + "/" + fileId + extension;
    }

    private WorkflowAttachmentRepository.FileRow requireFile(String fileId) {
        return repository.findFile(fileId).orElseThrow(() -> new BizException("文件不存在"));
    }

    private WorkflowStorageProvider requireProvider(String providerKey) {
        WorkflowStorageProvider provider = providers.get(providerKey);
        if (provider == null) {
            throw new BizException("没有可用的存储适配器: " + providerKey);
        }
        return provider;
    }

    private String normalizeSha(String sha256) {
        return StringUtils.hasText(sha256) ? sha256.trim().toLowerCase(Locale.ROOT) : null;
    }

    private String newId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public record DownloadObject(Resource resource, String fileName, String contentType, long fileSize) {
    }
}
