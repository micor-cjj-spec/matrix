-- Matrix unified notification platform - phase 1
-- MySQL 8.0+

CREATE TABLE IF NOT EXISTS im_message_task (
    id                  VARCHAR(32)  NOT NULL,
    message_no          VARCHAR(64)  NOT NULL,
    tenant_id           VARCHAR(64)  NOT NULL DEFAULT 'default',
    app_code            VARCHAR(64)  NOT NULL,
    request_id          VARCHAR(128) NOT NULL,
    message_type        VARCHAR(64)  NOT NULL,
    template_code       VARCHAR(64)  NULL,
    title               VARCHAR(255) NULL,
    content             TEXT         NULL,
    priority            VARCHAR(16)  NOT NULL DEFAULT 'NORMAL',
    scheduled_time      DATETIME(3)  NOT NULL,
    expire_time         DATETIME(3)  NULL,
    business_type       VARCHAR(64)  NULL,
    business_id         VARCHAR(128) NULL,
    action_url          VARCHAR(1000) NULL,
    status              VARCHAR(32)  NOT NULL,
    total_channels      INT          NOT NULL DEFAULT 0,
    success_channels    INT          NOT NULL DEFAULT 0,
    failed_channels     INT          NOT NULL DEFAULT 0,
    callback_url        VARCHAR(1000) NULL,
    callback_status     VARCHAR(32)   NULL,
    created_time        DATETIME(3)  NOT NULL,
    updated_time        DATETIME(3)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_im_message_no (message_no),
    UNIQUE KEY uk_im_app_request (app_code, request_id),
    KEY idx_im_message_status_time (status, scheduled_time),
    KEY idx_im_message_business (business_type, business_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统一消息主任务';

CREATE TABLE IF NOT EXISTS im_message_recipient (
    id                  VARCHAR(32)  NOT NULL,
    message_id          VARCHAR(32)  NOT NULL,
    receiver_type       VARCHAR(32)  NOT NULL,
    receiver_id         VARCHAR(128) NOT NULL,
    receiver_name       VARCHAR(128) NULL,
    email               VARCHAR(255) NULL,
    read_status         VARCHAR(32)  NOT NULL DEFAULT 'UNREAD',
    read_time           DATETIME(3)  NULL,
    created_time        DATETIME(3)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_im_recipient_message (message_id),
    KEY idx_im_recipient_receiver (receiver_type, receiver_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息接收人';

CREATE TABLE IF NOT EXISTS im_channel_task (
    id                       VARCHAR(32)  NOT NULL,
    message_id               VARCHAR(32)  NOT NULL,
    recipient_id             VARCHAR(32)  NOT NULL,
    channel_type             VARCHAR(32)  NOT NULL,
    subject                  VARCHAR(255) NOT NULL,
    content                  LONGTEXT     NOT NULL,
    status                   VARCHAR(32)  NOT NULL,
    retry_count              INT          NOT NULL DEFAULT 0,
    max_retry_count          INT          NOT NULL DEFAULT 5,
    next_retry_time          DATETIME(3)  NULL,
    provider_code            VARCHAR(64)  NULL,
    provider_message_id      VARCHAR(128) NULL,
    last_error_code          VARCHAR(64)  NULL,
    last_error_message       VARCHAR(1000) NULL,
    sent_time                DATETIME(3)  NULL,
    delivered_time           DATETIME(3)  NULL,
    processing_started_time  DATETIME(3)  NULL,
    created_time             DATETIME(3)  NOT NULL,
    updated_time             DATETIME(3)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_im_recipient_channel (message_id, recipient_id, channel_type),
    KEY idx_im_channel_retry (status, next_retry_time),
    KEY idx_im_channel_processing (status, processing_started_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='渠道发送任务';

CREATE TABLE IF NOT EXISTS im_local_notification (
    id                  VARCHAR(32)  NOT NULL,
    message_id          VARCHAR(32)  NOT NULL,
    recipient_id        VARCHAR(32)  NOT NULL,
    user_id             VARCHAR(128) NOT NULL,
    title               VARCHAR(255) NOT NULL,
    content             LONGTEXT     NOT NULL,
    message_type        VARCHAR(64)  NOT NULL,
    business_type       VARCHAR(64)  NULL,
    business_id         VARCHAR(128) NULL,
    action_url          VARCHAR(1000) NULL,
    push_status         VARCHAR(32)  NOT NULL DEFAULT 'CREATED',
    read_status         VARCHAR(32)  NOT NULL DEFAULT 'UNREAD',
    read_time           DATETIME(3)  NULL,
    created_time        DATETIME(3)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_im_local_message_user (message_id, user_id),
    KEY idx_im_local_user_unread (user_id, read_status, created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='本地站内提醒';

CREATE TABLE IF NOT EXISTS im_message_template (
    id                       VARCHAR(32)  NOT NULL,
    template_code            VARCHAR(64)  NOT NULL,
    template_name            VARCHAR(128) NOT NULL,
    message_type             VARCHAR(64)  NOT NULL,
    local_title_template     VARCHAR(255) NULL,
    local_body_template      LONGTEXT     NULL,
    email_subject_template   VARCHAR(255) NULL,
    email_body_template      LONGTEXT     NULL,
    default_channels         VARCHAR(255) NOT NULL,
    version                  INT          NOT NULL,
    status                   VARCHAR(32)  NOT NULL DEFAULT 'ENABLED',
    created_time             DATETIME(3)  NOT NULL,
    updated_time             DATETIME(3)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_im_template_version (template_code, version),
    KEY idx_im_template_enabled (template_code, status, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息模板';

CREATE TABLE IF NOT EXISTS im_outbox_event (
    id                  VARCHAR(32)  NOT NULL,
    event_id            VARCHAR(160) NOT NULL,
    aggregate_type      VARCHAR(64)  NOT NULL,
    aggregate_id        VARCHAR(64)  NOT NULL,
    event_type          VARCHAR(64)  NOT NULL,
    payload_json        JSON         NOT NULL,
    status              VARCHAR(32)  NOT NULL,
    retry_count         INT          NOT NULL DEFAULT 0,
    next_retry_time     DATETIME(3)  NULL,
    last_error          VARCHAR(1000) NULL,
    created_time        DATETIME(3)  NOT NULL,
    published_time      DATETIME(3)  NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_im_outbox_event (event_id),
    KEY idx_im_outbox_due (status, next_retry_time, created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IM平台Outbox事件';

INSERT INTO im_message_template (
    id,template_code,template_name,message_type,
    local_title_template,local_body_template,
    email_subject_template,email_body_template,
    default_channels,version,status,created_time,updated_time
) VALUES (
    REPLACE(UUID(),'-',''),
    'TASK_EXECUTION_FAILED',
    '任务执行失败',
    'TASK_FAILED',
    '任务执行失败',
    '任务「${taskName}」执行失败，失败原因：${errorMessage}',
    '【Matrix】任务执行失败：${taskName}',
    '<h3>任务执行失败</h3><p>任务：${taskName}</p><p>失败原因：${errorMessage}</p>',
    'LOCAL,EMAIL',
    1,
    'ENABLED',
    NOW(3),
    NOW(3)
) ON DUPLICATE KEY UPDATE updated_time=VALUES(updated_time);
