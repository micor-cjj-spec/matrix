CREATE TABLE IF NOT EXISTS fi_expense_reimbursement (
    id VARCHAR(40) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    document_number VARCHAR(80) NOT NULL,
    applicant_id VARCHAR(64) NOT NULL,
    department_code VARCHAR(64) NOT NULL,
    amount DECIMAL(19, 4) NOT NULL,
    currency_code VARCHAR(16) NOT NULL DEFAULT 'CNY',
    description_text VARCHAR(1000) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    workflow_instance_id VARCHAR(40) NULL,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    submitted_at DATETIME NULL,
    completed_at DATETIME NULL,
    UNIQUE KEY uk_fi_expense_number (tenant_id, document_number),
    KEY idx_fi_expense_status (tenant_id, status, created_at),
    KEY idx_fi_expense_workflow (workflow_instance_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS fi_workflow_binding (
    id VARCHAR(40) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    business_type VARCHAR(100) NOT NULL,
    business_id VARCHAR(128) NOT NULL,
    workflow_instance_id VARCHAR(40) NULL,
    workflow_definition_key VARCHAR(100) NOT NULL,
    workflow_status VARCHAR(32) NOT NULL,
    business_status VARCHAR(32) NOT NULL,
    submit_request_id VARCHAR(160) NOT NULL,
    callback_url VARCHAR(500) NOT NULL,
    submitted_by VARCHAR(64) NOT NULL,
    submitted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at DATETIME NULL,
    version INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_fi_workflow_business (tenant_id, business_type, business_id),
    KEY idx_fi_workflow_instance (workflow_instance_id),
    KEY idx_fi_workflow_status (tenant_id, workflow_status, submitted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS fi_event_outbox (
    id VARCHAR(40) PRIMARY KEY,
    event_id VARCHAR(40) NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(128) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    payload_json JSON NOT NULL,
    idempotency_key VARCHAR(200) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    next_retry_time DATETIME NULL,
    last_error VARCHAR(1000) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at DATETIME NULL,
    UNIQUE KEY uk_fi_outbox_event (event_id),
    UNIQUE KEY uk_fi_outbox_idempotency (idempotency_key),
    KEY idx_fi_outbox_dispatch (status, next_retry_time, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS fi_workflow_event_log (
    event_id VARCHAR(40) PRIMARY KEY,
    event_type VARCHAR(64) NOT NULL,
    workflow_instance_id VARCHAR(40) NOT NULL,
    business_type VARCHAR(100) NOT NULL,
    business_id VARCHAR(128) NOT NULL,
    processed_status VARCHAR(32) NOT NULL,
    error_message VARCHAR(1000) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at DATETIME NULL,
    KEY idx_fi_workflow_event_business (business_type, business_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
