CREATE TABLE IF NOT EXISTS fi_workflow_reconciliation_issue (
    id VARCHAR(40) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    business_type VARCHAR(100) NOT NULL,
    business_id VARCHAR(128) NOT NULL,
    workflow_instance_id VARCHAR(40) NULL,
    issue_type VARCHAR(64) NOT NULL,
    old_business_status VARCHAR(32) NULL,
    old_workflow_status VARCHAR(32) NULL,
    actual_business_status VARCHAR(32) NULL,
    actual_workflow_status VARCHAR(32) NULL,
    resolve_status VARCHAR(32) NOT NULL,
    detail_text VARCHAR(1000) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_fi_reconciliation_issue (
        tenant_id, business_type, business_id, issue_type, resolve_status
    ),
    KEY idx_fi_reconciliation_status (resolve_status, updated_at),
    KEY idx_fi_reconciliation_business (tenant_id, business_type, business_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
