package single.cjj.workflow.attachment;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class WorkflowAttachmentRepository {

    private final JdbcTemplate jdbcTemplate;

    public WorkflowAttachmentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insertFile(FileRow row) {
        jdbcTemplate.update("""
                INSERT INTO wf_file
                    (id, tenant_id, storage_provider, bucket_name, object_key,
                     original_name, content_type, expected_size, file_size,
                     expected_sha256, sha256, upload_status, scan_status,
                     created_by, version, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0, ?, NULL, 'PENDING', 'PENDING', ?, 0, ?)
                """, row.id(), row.tenantId(), row.storageProvider(), row.bucketName(),
                row.objectKey(), row.originalName(), row.contentType(), row.expectedSize(),
                row.expectedSha256(), row.createdBy(), row.createdAt());
    }

    public void insertRelation(RelationRow row) {
        jdbcTemplate.update("""
                INSERT INTO wf_attachment_relation
                    (id, file_id, tenant_id, source_system, business_type, business_id,
                     instance_id, task_id, category_code, status, created_by, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', ?, ?)
                """, row.id(), row.fileId(), row.tenantId(), row.sourceSystem(), row.businessType(),
                row.businessId(), row.instanceId(), row.taskId(), row.categoryCode(),
                row.createdBy(), row.createdAt());
    }

    public Optional<FileRow> findFile(String fileId) {
        return first(jdbcTemplate.query("SELECT * FROM wf_file WHERE id = ?", this::mapFile, fileId));
    }

    public int markStored(String fileId, long fileSize, String sha256) {
        return jdbcTemplate.update("""
                UPDATE wf_file
                SET upload_status = 'STORED', file_size = ?, sha256 = ?, version = version + 1
                WHERE id = ? AND upload_status = 'PENDING'
                """, fileSize, sha256, fileId);
    }

    public int confirmUpload(String fileId, int expectedVersion) {
        return jdbcTemplate.update("""
                UPDATE wf_file
                SET upload_status = 'UPLOADED', scan_status = 'CLEAN',
                    uploaded_at = NOW(), version = version + 1
                WHERE id = ? AND upload_status = 'STORED' AND version = ?
                """, fileId, expectedVersion);
    }

    public List<AttachmentRow> listByBusiness(String tenantId,
                                               String sourceSystem,
                                               String businessType,
                                               String businessId) {
        return jdbcTemplate.query("""
                SELECT r.id relation_id, r.file_id, r.tenant_id, r.source_system,
                       r.business_type, r.business_id, r.instance_id, r.task_id,
                       r.category_code, r.created_at relation_created_at, f.*
                FROM wf_attachment_relation r
                JOIN wf_file f ON f.id = r.file_id
                WHERE r.tenant_id = ? AND r.source_system = ?
                  AND r.business_type = ? AND r.business_id = ?
                  AND r.status = 'ACTIVE' AND f.upload_status = 'UPLOADED'
                ORDER BY r.created_at
                """, this::mapAttachment, tenantId, sourceSystem, businessType, businessId);
    }

    public List<AttachmentRow> listByInstance(String instanceId) {
        return jdbcTemplate.query("""
                SELECT r.id relation_id, r.file_id, r.tenant_id, r.source_system,
                       r.business_type, r.business_id, r.instance_id, r.task_id,
                       r.category_code, r.created_at relation_created_at, f.*
                FROM wf_attachment_relation r
                JOIN wf_file f ON f.id = r.file_id
                JOIN wf_instance i ON i.id = ?
                WHERE r.status = 'ACTIVE' AND f.upload_status = 'UPLOADED'
                  AND (
                    r.instance_id = i.id
                    OR (r.tenant_id = i.tenant_id
                        AND r.source_system = i.source_system
                        AND r.business_type = i.business_type
                        AND r.business_id = i.business_id)
                  )
                ORDER BY r.created_at
                """, this::mapAttachment, instanceId);
    }

    public Map<String, Long> countUploadedCategories(String instanceId) {
        Map<String, Long> result = new LinkedHashMap<>();
        jdbcTemplate.query("""
                SELECT r.category_code, COUNT(*) attachment_count
                FROM wf_attachment_relation r
                JOIN wf_file f ON f.id = r.file_id
                JOIN wf_instance i ON i.id = ?
                WHERE r.status = 'ACTIVE'
                  AND f.upload_status = 'UPLOADED'
                  AND f.scan_status = 'CLEAN'
                  AND (
                    r.instance_id = i.id
                    OR (r.tenant_id = i.tenant_id
                        AND r.source_system = i.source_system
                        AND r.business_type = i.business_type
                        AND r.business_id = i.business_id)
                  )
                GROUP BY r.category_code
                """, rs -> result.put(rs.getString("category_code"), rs.getLong("attachment_count")), instanceId);
        return result;
    }

    public int deactivateRelation(String relationId, String operatorId) {
        return jdbcTemplate.update("""
                UPDATE wf_attachment_relation
                SET status = 'DELETED', deleted_by = ?, deleted_at = NOW()
                WHERE id = ? AND status = 'ACTIVE'
                """, operatorId, relationId);
    }

    public boolean hasActiveRelation(String fileId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM wf_attachment_relation WHERE file_id = ? AND status = 'ACTIVE'",
                Integer.class, fileId);
        return count != null && count > 0;
    }

    public Optional<RelationRow> findRelation(String relationId) {
        return first(jdbcTemplate.query("SELECT * FROM wf_attachment_relation WHERE id = ?", (rs, rowNum) ->
                new RelationRow(
                        rs.getString("id"), rs.getString("file_id"), rs.getString("tenant_id"),
                        rs.getString("source_system"), rs.getString("business_type"),
                        rs.getString("business_id"), rs.getString("instance_id"),
                        rs.getString("task_id"), rs.getString("category_code"),
                        rs.getString("created_by"), rs.getObject("created_at", LocalDateTime.class)
                ), relationId));
    }

    private FileRow mapFile(ResultSet rs, int rowNum) throws SQLException {
        return new FileRow(
                rs.getString("id"), rs.getString("tenant_id"), rs.getString("storage_provider"),
                rs.getString("bucket_name"), rs.getString("object_key"), rs.getString("original_name"),
                rs.getString("content_type"), rs.getLong("expected_size"), rs.getLong("file_size"),
                rs.getString("expected_sha256"), rs.getString("sha256"), rs.getString("upload_status"),
                rs.getString("scan_status"), rs.getString("created_by"), rs.getInt("version"),
                rs.getObject("created_at", LocalDateTime.class), rs.getObject("uploaded_at", LocalDateTime.class)
        );
    }

    private AttachmentRow mapAttachment(ResultSet rs, int rowNum) throws SQLException {
        return new AttachmentRow(
                rs.getString("relation_id"), rs.getString("file_id"), rs.getString("tenant_id"),
                rs.getString("source_system"), rs.getString("business_type"), rs.getString("business_id"),
                rs.getString("instance_id"), rs.getString("task_id"), rs.getString("category_code"),
                rs.getString("original_name"), rs.getString("content_type"), rs.getLong("file_size"),
                rs.getString("sha256"), rs.getString("upload_status"), rs.getString("scan_status"),
                rs.getString("object_key"), rs.getString("created_by"),
                rs.getObject("relation_created_at", LocalDateTime.class),
                rs.getObject("uploaded_at", LocalDateTime.class)
        );
    }

    private <T> Optional<T> first(List<T> rows) {
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public record FileRow(
            String id, String tenantId, String storageProvider, String bucketName, String objectKey,
            String originalName, String contentType, long expectedSize, long fileSize,
            String expectedSha256, String sha256, String uploadStatus, String scanStatus,
            String createdBy, int version, LocalDateTime createdAt, LocalDateTime uploadedAt
    ) {
    }

    public record RelationRow(
            String id, String fileId, String tenantId, String sourceSystem, String businessType,
            String businessId, String instanceId, String taskId, String categoryCode,
            String createdBy, LocalDateTime createdAt
    ) {
    }

    public record AttachmentRow(
            String relationId, String fileId, String tenantId, String sourceSystem,
            String businessType, String businessId, String instanceId, String taskId,
            String categoryCode, String originalName, String contentType, long fileSize,
            String sha256, String uploadStatus, String scanStatus, String objectKey,
            String createdBy, LocalDateTime createdAt, LocalDateTime uploadedAt
    ) {
    }
}
