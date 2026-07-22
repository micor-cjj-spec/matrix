CREATE TABLE IF NOT EXISTS matrix_fi_scheduler_report_snapshot (
    fid BIGINT NOT NULL,
    fexecution_no VARCHAR(64) NOT NULL,
    freport_type VARCHAR(64) NOT NULL,
    fperiod VARCHAR(7) NOT NULL,
    fbook_id VARCHAR(64) NULL,
    fstatus VARCHAR(32) NOT NULL,
    fsummary_json JSON NOT NULL,
    fcreated_time DATETIME NOT NULL,
    PRIMARY KEY (fid),
    UNIQUE KEY uk_fi_scheduler_report_execution (fexecution_no),
    KEY idx_fi_scheduler_report_period (fperiod, freport_type)
) ENGINE=InnoDB;
