-- Matrix OpenAPI V3
-- 第三阶段A：凭证草稿异步写入、幂等、Outbox与状态跟踪
-- 前置：matrix_open_api_v1.sql、matrix_open_api_v2.sql

ALTER TABLE bizfi_fi_voucher
    ADD COLUMN source_request_id VARCHAR(64) NULL COMMENT 'OpenAPI写入来源请求' AFTER book_id,
    ADD UNIQUE KEY uk_voucher_openapi_source (tenant_id, source_request_id);

CREATE TABLE matrix_open_api_write_request (
    id BIGINT NOT NULL AUTO_INCREMENT,
    request_id VARCHAR(64) NOT NULL COMMENT '对外写入请求ID',
    app_id BIGINT NOT NULL COMMENT 'matrix_open_api_app.id',
    app_external_id VARCHAR(64) NOT NULL COMMENT '对外应用ID快照',
    tenant_id VARCHAR(64) NOT NULL,
    external_biz_no VARCHAR(128) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    request_body_hash CHAR(64) NOT NULL,
    organization_id VARCHAR(64) NOT NULL,
    book_id VARCHAR(64) NOT NULL,
    voucher_date DATE NOT NULL,
    summary VARCHAR(500) NOT NULL,
    status VARCHAR(32) NOT NULL COMMENT 'ACCEPTED/PROCESSING/RETRYING/SUCCEEDED/MANUAL_REQUIRED',
    voucher_id BIGINT NULL,
    voucher_number VARCHAR(100) NULL,
    error_code VARCHAR(64) NULL,
    error_message VARCHAR(1000) NULL,
    retry_count INT NOT NULL DEFAULT 0,
    max_retry INT NOT NULL DEFAULT 5,
    next_retry_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    finished_at DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_open_api_write_request_id (request_id),
    UNIQUE KEY uk_open_api_write_idempotency (app_id, idempotency_key),
    UNIQUE KEY uk_open_api_write_external_biz (app_id, external_biz_no),
    KEY idx_open_api_write_status_time (status, updated_at),
    KEY idx_open_api_write_app_time (app_external_id, created_at),
    KEY idx_open_api_write_voucher (voucher_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OpenAPI凭证写入任务';

CREATE TABLE matrix_open_api_write_request_line (
    id BIGINT NOT NULL AUTO_INCREMENT,
    write_request_id BIGINT NOT NULL,
    line_no INT NOT NULL,
    account_code VARCHAR(100) NOT NULL,
    summary VARCHAR(500) NULL,
    debit_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
    credit_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
    currency VARCHAR(20) NULL,
    rate DECIMAL(18,6) NULL,
    original_amount DECIMAL(18,2) NULL,
    cashflow_item VARCHAR(100) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_open_api_write_line_no (write_request_id, line_no),
    KEY idx_open_api_write_line_request (write_request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OpenAPI凭证写入分录';

CREATE TABLE matrix_open_api_outbox_event (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id VARCHAR(64) NOT NULL,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id BIGINT NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    payload_json JSON NULL,
    status VARCHAR(20) NOT NULL COMMENT 'PENDING/SENDING/SENT/FAILED/DEAD',
    retry_count INT NOT NULL DEFAULT 0,
    max_retry INT NOT NULL DEFAULT 10,
    next_attempt_at DATETIME NOT NULL,
    sent_at DATETIME NULL,
    error_message VARCHAR(1000) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_open_api_outbox_event_id (event_id),
    KEY idx_open_api_outbox_dispatch (status, next_attempt_at, id),
    KEY idx_open_api_outbox_aggregate (aggregate_type, aggregate_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OpenAPI事务Outbox';

CREATE TABLE matrix_open_api_write_status_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    write_request_id BIGINT NOT NULL,
    from_status VARCHAR(32) NULL,
    to_status VARCHAR(32) NOT NULL,
    error_code VARCHAR(64) NULL,
    message VARCHAR(1000) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_open_api_write_log_request (write_request_id, id),
    KEY idx_open_api_write_log_status (to_status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OpenAPI凭证写入状态轨迹';

INSERT INTO matrix_open_api_definition
(api_code, api_name, api_version, http_method, external_path, scope_code, status, max_page_size, sensitivity_level, description)
VALUES
('fi.voucher.write.request', '凭证草稿写入受理', 'v1', 'POST', '/open-api/v1/fi/voucher-requests', 'fi.voucher.write', 'PUBLISHED', NULL, 'FINANCIAL_WRITE', '幂等受理凭证草稿异步写入请求'),
('fi.voucher.write.status', '凭证写入状态查询', 'v1', 'GET', '/open-api/v1/fi/voucher-requests/{requestId}', 'fi.voucher.write.status.read', 'PUBLISHED', NULL, 'FINANCIAL', '按请求ID查询凭证写入状态'),
('fi.voucher.write.status.external', '凭证写入外部单号查询', 'v1', 'GET', '/open-api/v1/fi/voucher-requests/by-external-no/{externalBizNo}', 'fi.voucher.write.status.read', 'PUBLISHED', NULL, 'FINANCIAL', '按外部业务单号查询凭证写入状态')
ON DUPLICATE KEY UPDATE
api_name = VALUES(api_name),
http_method = VALUES(http_method),
external_path = VALUES(external_path),
scope_code = VALUES(scope_code),
status = VALUES(status),
description = VALUES(description);

-- 写入授权示例：
-- {
--   "organizationIds": ["ORG-001"],
--   "bookIds": ["BOOK-001"],
--   "maxLinesPerVoucher": 200,
--   "dailyWriteQuota": 10000
-- }
