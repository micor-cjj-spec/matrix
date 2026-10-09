CREATE TABLE IF NOT EXISTS wf_definition (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id VARCHAR(64) NOT NULL,
    definition_key VARCHAR(100) NOT NULL,
    definition_name VARCHAR(200) NOT NULL,
    latest_version INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_by VARCHAR(64) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_wf_definition (tenant_id, definition_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS wf_definition_version (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id VARCHAR(64) NOT NULL,
    definition_key VARCHAR(100) NOT NULL,
    definition_name VARCHAR(200) NOT NULL,
    version INT NOT NULL,
    definition_json JSON NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at DATETIME NULL,
    UNIQUE KEY uk_wf_definition_version (tenant_id, definition_key, version),
    KEY idx_wf_definition_published (tenant_id, definition_key, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS wf_instance (
    id VARCHAR(40) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    definition_key VARCHAR(100) NOT NULL,
    definition_version INT NOT NULL,
    source_system VARCHAR(100) NOT NULL,
    business_type VARCHAR(100) NOT NULL,
    business_id VARCHAR(128) NOT NULL,
    business_key VARCHAR(360) NOT NULL,
    idempotency_key VARCHAR(160) NOT NULL,
    initiator_id VARCHAR(64) NOT NULL,
    current_node_key VARCHAR(100) NULL,
    status VARCHAR(32) NOT NULL,
    variables_json JSON NOT NULL,
    callback_url VARCHAR(500) NULL,
    version INT NOT NULL DEFAULT 0,
    started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at DATETIME NULL,
    UNIQUE KEY uk_wf_instance_idempotency (tenant_id, source_system, idempotency_key),
    KEY idx_wf_instance_business (tenant_id, source_system, business_type, business_id),
    KEY idx_wf_instance_status (tenant_id, status, started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS wf_node_instance (
    id VARCHAR(40) PRIMARY KEY,
    instance_id VARCHAR(40) NOT NULL,
    node_key VARCHAR(100) NOT NULL,
    node_name VARCHAR(200) NOT NULL,
    node_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    handler_key VARCHAR(100) NULL,
    input_json JSON NULL,
    output_json JSON NULL,
    started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at DATETIME NULL,
    KEY idx_wf_node_instance (instance_id, node_key, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS wf_task (
    id VARCHAR(40) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    instance_id VARCHAR(40) NOT NULL,
    node_instance_id VARCHAR(40) NOT NULL,
    node_key VARCHAR(100) NOT NULL,
    task_name VARCHAR(200) NOT NULL,
    assignee_type VARCHAR(32) NOT NULL,
    assignee_value VARCHAR(200) NOT NULL,
    status VARCHAR(32) NOT NULL,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at DATETIME NULL,
    KEY idx_wf_task_assignee (tenant_id, assignee_type, assignee_value, status, created_at),
    KEY idx_wf_task_instance (instance_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS wf_action_log (
    id VARCHAR(40) PRIMARY KEY,
    instance_id VARCHAR(40) NOT NULL,
    task_id VARCHAR(40) NULL,
    action VARCHAR(32) NOT NULL,
    operator_id VARCHAR(64) NOT NULL,
    comment_text VARCHAR(1000) NULL,
    before_status VARCHAR(32) NULL,
    after_status VARCHAR(32) NULL,
    request_id VARCHAR(160) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_wf_action_instance (instance_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS wf_event_outbox (
    id VARCHAR(40) PRIMARY KEY,
    event_id VARCHAR(40) NOT NULL,
    instance_id VARCHAR(40) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    payload_json JSON NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    next_retry_time DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at DATETIME NULL,
    UNIQUE KEY uk_wf_event_id (event_id),
    KEY idx_wf_outbox_dispatch (status, next_retry_time, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
