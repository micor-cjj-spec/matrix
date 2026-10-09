USE matrix_scheduler;

ALTER TABLE matrix_scheduler_execution
    ADD COLUMN fprogress INT NOT NULL DEFAULT 0 AFTER fdeadline_time,
    ADD COLUMN fcurrent_stage VARCHAR(64) NULL AFTER fprogress,
    ADD COLUMN fprogress_message VARCHAR(500) NULL AFTER fcurrent_stage,
    ADD COLUMN flast_progress_time DATETIME NULL AFTER fprogress_message;

CREATE TABLE IF NOT EXISTS matrix_scheduler_operation_log (
    fid BIGINT NOT NULL,
    fexecution_no VARCHAR(64) NOT NULL,
    faction VARCHAR(32) NOT NULL,
    foperator_id VARCHAR(64) NOT NULL,
    freason VARCHAR(500) NOT NULL,
    ffrom_status VARCHAR(32) NULL,
    fto_status VARCHAR(32) NULL,
    fcreate_time DATETIME NOT NULL,
    PRIMARY KEY (fid),
    KEY idx_scheduler_operation_execution (fexecution_no, fcreate_time),
    KEY idx_scheduler_operation_operator (foperator_id, fcreate_time)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS matrix_scheduler_alert_record (
    fid BIGINT NOT NULL,
    fdedupe_key VARCHAR(255) NOT NULL,
    fexecution_no VARCHAR(64) NULL,
    fjob_id BIGINT NULL,
    fexecutor_code VARCHAR(64) NULL,
    falert_type VARCHAR(64) NOT NULL,
    flevel VARCHAR(16) NOT NULL,
    ftitle VARCHAR(200) NOT NULL,
    fcontent VARCHAR(2000) NOT NULL,
    fstatus VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    fack_by VARCHAR(64) NULL,
    fack_time DATETIME NULL,
    fcreate_time DATETIME NOT NULL,
    fupdate_time DATETIME NOT NULL,
    PRIMARY KEY (fid),
    UNIQUE KEY uk_scheduler_alert_dedupe (fdedupe_key),
    KEY idx_scheduler_alert_status_time (fstatus, fcreate_time),
    KEY idx_scheduler_alert_job_time (fjob_id, fcreate_time)
) ENGINE=InnoDB;
