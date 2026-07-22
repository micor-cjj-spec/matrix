-- Matrix OpenAPI V1
-- 第一阶段：凭证只读开放能力

CREATE TABLE IF NOT EXISTS matrix_open_api_app (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    app_id VARCHAR(64) NOT NULL COMMENT '对外应用ID',
    app_name VARCHAR(200) NOT NULL COMMENT '应用名称',
    app_key VARCHAR(128) NOT NULL COMMENT '调用方标识',
    app_secret_cipher TEXT NOT NULL COMMENT 'AES-GCM密文',
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '租户',
    status VARCHAR(30) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
    valid_from DATETIME NULL,
    valid_to DATETIME NULL,
    ip_whitelist VARCHAR(2000) NULL COMMENT '逗号分隔IP/CIDR，第一期支持精确IP和*',
    qps_limit INT NOT NULL DEFAULT 10,
    max_page_size INT NOT NULL DEFAULT 200,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_open_api_app_id (app_id),
    UNIQUE KEY uk_open_api_app_key (app_key),
    KEY idx_open_api_app_status (status, valid_to)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OpenAPI外部应用';

CREATE TABLE IF NOT EXISTS matrix_open_api_definition (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    api_code VARCHAR(100) NOT NULL COMMENT '稳定API编码',
    api_name VARCHAR(200) NOT NULL,
    api_version VARCHAR(20) NOT NULL DEFAULT 'v1',
    http_method VARCHAR(10) NOT NULL,
    external_path VARCHAR(255) NOT NULL COMMENT 'Ant风格外部路径',
    scope_code VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED/OFFLINE',
    max_page_size INT NULL,
    sensitivity_level VARCHAR(30) NOT NULL DEFAULT 'FINANCIAL',
    description VARCHAR(1000) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_open_api_code_version (api_code, api_version),
    KEY idx_open_api_route (http_method, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OpenAPI定义';

CREATE TABLE IF NOT EXISTS matrix_open_api_grant (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    app_id BIGINT NOT NULL COMMENT 'matrix_open_api_app.id',
    api_definition_id BIGINT NOT NULL COMMENT 'matrix_open_api_definition.id',
    status VARCHAR(30) NOT NULL DEFAULT 'ENABLED',
    data_permission_json JSON NULL COMMENT 'allowedStatuses/maxHistoryMonths等',
    field_permission_json JSON NULL,
    valid_from DATETIME NULL,
    valid_to DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_open_api_app_api (app_id, api_definition_id),
    KEY idx_open_api_grant_status (status, valid_to)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OpenAPI应用授权';

CREATE TABLE IF NOT EXISTS matrix_open_api_request_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    request_id VARCHAR(64) NOT NULL,
    app_id VARCHAR(64) NULL,
    api_code VARCHAR(100) NULL,
    api_version VARCHAR(20) NULL,
    http_method VARCHAR(10) NULL,
    request_path VARCHAR(255) NULL,
    client_ip VARCHAR(64) NULL,
    response_code VARCHAR(64) NULL,
    http_status INT NULL,
    success TINYINT(1) NOT NULL DEFAULT 0,
    duration_ms BIGINT NULL,
    request_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    response_time DATETIME NULL,
    error_message VARCHAR(1000) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_open_api_request_id (request_id),
    KEY idx_open_api_log_app_time (app_id, request_time),
    KEY idx_open_api_log_api_time (api_code, request_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OpenAPI调用日志';

INSERT INTO matrix_open_api_definition
(api_code, api_name, api_version, http_method, external_path, scope_code, status, max_page_size, sensitivity_level, description)
VALUES
('fi.voucher.list', '凭证列表查询', 'v1', 'GET', '/open-api/v1/fi/vouchers', 'fi.voucher.read', 'PUBLISHED', 200, 'FINANCIAL', '分页查询已授权范围内的凭证'),
('fi.voucher.detail', '凭证详情查询', 'v1', 'GET', '/open-api/v1/fi/vouchers/{voucherId}', 'fi.voucher.read', 'PUBLISHED', NULL, 'FINANCIAL', '查询单张凭证详情'),
('fi.voucher.lines', '凭证分录查询', 'v1', 'GET', '/open-api/v1/fi/vouchers/{voucherId}/lines', 'fi.voucher.lines.read', 'PUBLISHED', NULL, 'FINANCIAL', '查询单张凭证分录')
ON DUPLICATE KEY UPDATE
api_name = VALUES(api_name),
http_method = VALUES(http_method),
external_path = VALUES(external_path),
scope_code = VALUES(scope_code),
status = VALUES(status),
max_page_size = VALUES(max_page_size),
description = VALUES(description);
