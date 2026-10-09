USE matrix_scheduler;

ALTER TABLE matrix_scheduler_execution
    ADD COLUMN froot_execution_id BIGINT NULL AFTER fattempt_no,
    ADD COLUMN fparent_execution_id BIGINT NULL AFTER froot_execution_id,
    ADD COLUMN fnext_retry_time DATETIME NULL AFTER fparent_execution_id,
    ADD COLUMN fdeadline_time DATETIME NULL AFTER fnext_retry_time,
    ADD KEY idx_scheduler_execution_retry (fstatus, fnext_retry_time),
    ADD KEY idx_scheduler_execution_deadline (fstatus, fdeadline_time),
    ADD KEY idx_scheduler_execution_root (froot_execution_id, fattempt_no);

CREATE TABLE IF NOT EXISTS matrix_scheduler_executor_instance (
    fid BIGINT NOT NULL,
    fexecutor_code VARCHAR(64) NOT NULL,
    finstance_id VARCHAR(160) NOT NULL,
    fstatus VARCHAR(32) NOT NULL,
    fmax_concurrency INT NOT NULL DEFAULT 10,
    frunning_count INT NOT NULL DEFAULT 0,
    flast_heartbeat_time DATETIME NOT NULL,
    fcreate_time DATETIME NOT NULL,
    fupdate_time DATETIME NOT NULL,
    PRIMARY KEY (fid),
    UNIQUE KEY uk_scheduler_executor_instance (fexecutor_code, finstance_id),
    KEY idx_scheduler_executor_heartbeat (fstatus, flast_heartbeat_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS matrix_scheduler_handler (
    fid BIGINT NOT NULL,
    fexecutor_code VARCHAR(64) NOT NULL,
    fhandler_code VARCHAR(128) NOT NULL,
    fhandler_name VARCHAR(160) NOT NULL,
    fstatus VARCHAR(32) NOT NULL,
    fcreate_time DATETIME NOT NULL,
    fupdate_time DATETIME NOT NULL,
    PRIMARY KEY (fid),
    UNIQUE KEY uk_scheduler_executor_handler (fexecutor_code, fhandler_code),
    KEY idx_scheduler_handler_status (fexecutor_code, fstatus)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
