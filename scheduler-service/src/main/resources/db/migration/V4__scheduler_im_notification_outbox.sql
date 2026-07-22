USE matrix_scheduler;

CREATE TABLE IF NOT EXISTS matrix_scheduler_im_outbox (
    fid                       BIGINT NOT NULL,
    falert_id                 BIGINT NOT NULL,
    frequest_id               VARCHAR(128) NOT NULL,
    fpayload                  LONGTEXT NOT NULL,
    fstatus                   VARCHAR(32) NOT NULL,
    fretry_count              INT NOT NULL DEFAULT 0,
    fnext_retry_time          DATETIME NULL,
    fprocessing_started_time  DATETIME NULL,
    fmessage_no               VARCHAR(64) NULL,
    fcallback_status          VARCHAR(32) NULL,
    fcallback_event_id        VARCHAR(160) NULL,
    flast_error               VARCHAR(2000) NULL,
    fcreate_time              DATETIME NOT NULL,
    fupdate_time              DATETIME NOT NULL,
    PRIMARY KEY (fid),
    UNIQUE KEY uk_scheduler_im_request (frequest_id),
    UNIQUE KEY uk_scheduler_im_callback_event (fcallback_event_id),
    KEY idx_scheduler_im_due (fstatus, fnext_retry_time, fcreate_time),
    KEY idx_scheduler_im_processing (fstatus, fprocessing_started_time),
    KEY idx_scheduler_im_alert (falert_id)
) ENGINE=InnoDB COMMENT='Scheduler 到 IM 平台的可靠通知 Outbox';
