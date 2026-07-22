-- Matrix OpenAPI V4
-- 第三阶段B：结果回调、回调重试、每日对账与异常处理台
-- 前置：matrix_open_api_v1.sql、matrix_open_api_v2.sql、matrix_open_api_v3.sql

ALTER TABLE matrix_open_api_app
    ADD COLUMN callback_enabled TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否启用结果回调' AFTER max_page_size,
    ADD COLUMN callback_url VARCHAR(1000) NULL COMMENT '固定HTTPS回调地址' AFTER callback_enabled;

CREATE TABLE matrix_open_api_callback_task (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id VARCHAR(64) NOT NULL,
    write_request_id BIGINT NOT NULL,
    request_id VARCHAR(64) NOT NULL,
    app_id BIGINT NOT NULL,
    callback_url VARCHAR(1000) NOT NULL,
    event_type VARCHAR(64) NOT NULL COMMENT 'VOUCHER_WRITE_SUCCEEDED/VOUCHER_WRITE_MANUAL_REQUIRED',
    payload_json JSON NOT NULL,
    status VARCHAR(20) NOT NULL COMMENT 'PENDING/SENDING/SUCCEEDED/FAILED/DEAD/SKIPPED',
    retry_count INT NOT NULL DEFAULT 0,
    max_retry INT NOT NULL DEFAULT 6,
    next_attempt_at DATETIME NULL,
    last_http_status INT NULL,
    error_message VARCHAR(1000) NULL,
    sent_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_open_api_callback_event (event_id),
    UNIQUE KEY uk_open_api_callback_write_type (write_request_id, event_type),
    KEY idx_open_api_callback_dispatch (status, next_attempt_at, id),
    KEY idx_open_api_callback_request (request_id, created_at),
    KEY idx_open_api_callback_app (app_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OpenAPI结果回调任务';

CREATE TABLE matrix_open_api_reconcile_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    record_id VARCHAR(64) NOT NULL,
    issue_key VARCHAR(200) NOT NULL,
    issue_type VARCHAR(64) NOT NULL COMMENT 'VOUCHER_MISSING/TASK_STATUS_MISMATCH/VOUCHER_ID_MISMATCH/OUTBOX_STUCK/CALLBACK_DEAD/FINANCE_LOOKUP_FAILED',
    severity VARCHAR(20) NOT NULL COMMENT 'WARNING/HIGH/CRITICAL',
    write_request_id BIGINT NULL,
    request_id VARCHAR(64) NULL,
    app_id BIGINT NULL,
    expected_status VARCHAR(100) NULL,
    actual_status VARCHAR(100) NULL,
    detail_message VARCHAR(1000) NULL,
    status VARCHAR(20) NOT NULL COMMENT 'OPEN/RESOLVED',
    resolution VARCHAR(1000) NULL,
    resolved_by VARCHAR(100) NULL,
    detected_at DATETIME NOT NULL,
    resolved_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_open_api_reconcile_record (record_id),
    UNIQUE KEY uk_open_api_reconcile_issue (issue_key),
    KEY idx_open_api_reconcile_status (status, severity, detected_at),
    KEY idx_open_api_reconcile_request (request_id, detected_at),
    KEY idx_open_api_reconcile_app (app_id, detected_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OpenAPI对账异常记录';
