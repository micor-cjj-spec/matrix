CREATE DATABASE IF NOT EXISTS matrix_scheduler
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE matrix_scheduler;

CREATE TABLE IF NOT EXISTS matrix_scheduler_job (
    fid BIGINT NOT NULL,
    fjob_code VARCHAR(64) NOT NULL,
    fjob_name VARCHAR(128) NOT NULL,
    fsource_type VARCHAR(32) NOT NULL,
    fsource_service VARCHAR(64) NOT NULL,
    ftenant_id VARCHAR(64) DEFAULT 'default',
    fschedule_type VARCHAR(32) NOT NULL DEFAULT 'CRON',
    fcron_expression VARCHAR(128) NOT NULL,
    ftimezone VARCHAR(64) NOT NULL DEFAULT 'Asia/Shanghai',
    fexecute_type VARCHAR(32) NOT NULL,
    fexecutor_code VARCHAR(64) NOT NULL,
    fhandler_code VARCHAR(128) NOT NULL,
    fexecute_parameters JSON NULL,
    fstatus VARCHAR(32) NOT NULL,
    fconcurrency_policy VARCHAR(32) NOT NULL DEFAULT 'SKIP',
    fmisfire_policy VARCHAR(32) NOT NULL DEFAULT 'FIRE_ONCE_NOW',
    ftimeout_seconds INT NOT NULL DEFAULT 300,
    fretry_count INT NOT NULL DEFAULT 0,
    fretry_interval_seconds INT NOT NULL DEFAULT 60,
    fnext_fire_time DATETIME NULL,
    flast_fire_time DATETIME NULL,
    fidempotency_key VARCHAR(128) NULL,
    fversion INT NOT NULL DEFAULT 0,
    fcreate_by BIGINT NULL,
    fcreate_time DATETIME NOT NULL,
    fupdate_by BIGINT NULL,
    fupdate_time DATETIME NOT NULL,
    PRIMARY KEY (fid),
    UNIQUE KEY uk_scheduler_job_code (fjob_code),
    UNIQUE KEY uk_scheduler_source_request (fsource_service, fidempotency_key),
    KEY idx_scheduler_status_next (fstatus, fnext_fire_time),
    KEY idx_scheduler_tenant_status (ftenant_id, fstatus)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS matrix_scheduler_execution (
    fid BIGINT NOT NULL,
    fexecution_no VARCHAR(64) NOT NULL,
    fjob_id BIGINT NOT NULL,
    fjob_code VARCHAR(64) NOT NULL,
    fscheduled_time DATETIME NOT NULL,
    factual_start_time DATETIME NULL,
    factual_end_time DATETIME NULL,
    ftrigger_type VARCHAR(32) NOT NULL,
    fstatus VARCHAR(32) NOT NULL,
    fattempt_no INT NOT NULL DEFAULT 1,
    fexecutor_code VARCHAR(64) NULL,
    fhandler_code VARCHAR(128) NULL,
    fexecutor_instance VARCHAR(128) NULL,
    frequest_payload JSON NULL,
    fresponse_payload JSON NULL,
    ferror_code VARCHAR(64) NULL,
    ferror_message VARCHAR(2000) NULL,
    ftrace_id VARCHAR(64) NULL,
    fidempotency_key VARCHAR(255) NOT NULL,
    fcreate_time DATETIME NOT NULL,
    fupdate_time DATETIME NOT NULL,
    PRIMARY KEY (fid),
    UNIQUE KEY uk_scheduler_execution_no (fexecution_no),
    UNIQUE KEY uk_scheduler_execution_idempotency (fidempotency_key),
    KEY idx_scheduler_execution_job_time (fjob_id, fscheduled_time),
    KEY idx_scheduler_execution_status_time (fstatus, fcreate_time)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS matrix_scheduler_outbox (
    fid BIGINT NOT NULL,
    fevent_id VARCHAR(64) NOT NULL,
    fevent_type VARCHAR(64) NOT NULL,
    faggregate_id VARCHAR(64) NOT NULL,
    frouting_key VARCHAR(128) NOT NULL,
    fpayload JSON NOT NULL,
    fstatus VARCHAR(32) NOT NULL,
    fretry_count INT NOT NULL DEFAULT 0,
    fnext_retry_time DATETIME NULL,
    flast_error VARCHAR(2000) NULL,
    fcreate_time DATETIME NOT NULL,
    fupdate_time DATETIME NOT NULL,
    PRIMARY KEY (fid),
    UNIQUE KEY uk_scheduler_outbox_event (fevent_id),
    KEY idx_scheduler_outbox_retry (fstatus, fnext_retry_time)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS matrix_scheduler_executor (
    fid BIGINT NOT NULL,
    fexecutor_code VARCHAR(64) NOT NULL,
    fexecutor_name VARCHAR(128) NOT NULL,
    fexecute_type VARCHAR(32) NOT NULL,
    fservice_name VARCHAR(128) NULL,
    fbase_url VARCHAR(512) NULL,
    fauth_type VARCHAR(32) NULL,
    fsecret_ref VARCHAR(128) NULL,
    fstatus VARCHAR(32) NOT NULL,
    flast_heartbeat_time DATETIME NULL,
    fcreate_time DATETIME NOT NULL,
    fupdate_time DATETIME NOT NULL,
    PRIMARY KEY (fid),
    UNIQUE KEY uk_scheduler_executor_code (fexecutor_code)
) ENGINE=InnoDB;

-- Quartz 使用同一个数据源时，还需要执行 Quartz 2.x 官方 MySQL InnoDB 建表脚本。
-- 表前缀保持默认 QRTZ_，并确保所有 scheduler-service 实例共享这些表。
