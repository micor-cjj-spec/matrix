CREATE TABLE IF NOT EXISTS wf_file (
    id VARCHAR(40) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    storage_provider VARCHAR(32) NOT NULL,
    bucket_name VARCHAR(128) NOT NULL,
    object_key VARCHAR(500) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(128) NOT NULL,
    expected_size BIGINT NOT NULL,
    file_size BIGINT NOT NULL DEFAULT 0,
    expected_sha256 VARCHAR(64) NULL,
    sha256 VARCHAR(64) NULL,
    upload_status VARCHAR(32) NOT NULL,
    scan_status VARCHAR(32) NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    uploaded_at DATETIME NULL,
    UNIQUE KEY uk_wf_file_object (storage_provider, bucket_name, object_key),
    KEY idx_wf_file_tenant_status (tenant_id, upload_status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS wf_attachment_relation (
    id VARCHAR(40) PRIMARY KEY,
    file_id VARCHAR(40) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    source_system VARCHAR(100) NOT NULL,
    business_type VARCHAR(100) NOT NULL,
    business_id VARCHAR(128) NOT NULL,
    instance_id VARCHAR(40) NULL,
    task_id VARCHAR(40) NULL,
    category_code VARCHAR(100) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_by VARCHAR(64) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_by VARCHAR(64) NULL,
    deleted_at DATETIME NULL,
    KEY idx_wf_attachment_business
        (tenant_id, source_system, business_type, business_id, status),
    KEY idx_wf_attachment_instance (instance_id, status),
    KEY idx_wf_attachment_file (file_id, status),
    KEY idx_wf_attachment_category (category_code, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
