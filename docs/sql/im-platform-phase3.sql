-- Matrix unified notification platform - phase 3
-- Reliability hardening, dynamic applications, callbacks and dead letters
-- MySQL 8.0+

ALTER TABLE im_outbox_event
    ADD COLUMN processing_started_time DATETIME(3) NULL AFTER last_error,
    ADD COLUMN updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3) AFTER created_time,
    ADD KEY idx_im_outbox_processing (status, updated_time);

CREATE TABLE IF NOT EXISTS im_application (
    app_code                    VARCHAR(64)  NOT NULL,
    app_name                    VARCHAR(128) NOT NULL,
    tenant_id                   VARCHAR(64)  NOT NULL DEFAULT 'default',
    app_secret_ciphertext       VARCHAR(1000) NOT NULL,
    allowed_channels            VARCHAR(255) NOT NULL,
    allowed_ips                 VARCHAR(2000) NOT NULL DEFAULT '*',
    rate_limit_per_minute       INT NOT NULL DEFAULT 600,
    callback_url                VARCHAR(1000) NULL,
    callback_secret_ciphertext  VARCHAR(1000) NULL,
    status                      VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
    created_time                DATETIME(3) NOT NULL,
    updated_time                DATETIME(3) NOT NULL,
    PRIMARY KEY (app_code),
    KEY idx_im_application_tenant_status (tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IM 开放接口接入应用';

CREATE TABLE IF NOT EXISTS im_callback_task (
    id                          VARCHAR(32) NOT NULL,
    event_id                    VARCHAR(160) NOT NULL,
    message_id                  VARCHAR(32) NOT NULL,
    message_status              VARCHAR(32) NOT NULL,
    app_code                    VARCHAR(64) NOT NULL,
    callback_url                VARCHAR(1000) NOT NULL,
    callback_secret_ciphertext  VARCHAR(1000) NOT NULL,
    payload_json                JSON NOT NULL,
    status                      VARCHAR(32) NOT NULL,
    retry_count                 INT NOT NULL DEFAULT 0,
    max_retry_count             INT NOT NULL DEFAULT 6,
    next_retry_time             DATETIME(3) NULL,
    processing_started_time     DATETIME(3) NULL,
    response_code               INT NULL,
    response_body               VARCHAR(2000) NULL,
    last_error                  VARCHAR(1000) NULL,
    created_time                DATETIME(3) NOT NULL,
    updated_time                DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_im_callback_event (event_id),
    UNIQUE KEY uk_im_callback_message_status (message_id, message_status),
    KEY idx_im_callback_due (status, next_retry_time, created_time),
    KEY idx_im_callback_processing (status, processing_started_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息最终结果回调任务';

CREATE TABLE IF NOT EXISTS im_dead_letter (
    id              VARCHAR(32) NOT NULL,
    queue_name      VARCHAR(128) NOT NULL,
    message_id      VARCHAR(160) NOT NULL,
    payload_json    LONGTEXT NOT NULL,
    reason          VARCHAR(1000) NULL,
    status          VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    created_time    DATETIME(3) NOT NULL,
    updated_time    DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_im_dead_letter_message (queue_name, message_id),
    KEY idx_im_dead_letter_status_time (status, created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IM RabbitMQ 死信持久化记录';

-- 应用密钥必须通过 POST /api/im/applications 创建或更新。
-- API 只在创建/轮换时返回一次明文密钥，数据库保存 AES-GCM 密文。
