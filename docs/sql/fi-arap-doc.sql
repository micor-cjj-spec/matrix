CREATE TABLE IF NOT EXISTS bizfi_fi_arap_doc (
  fid BIGINT PRIMARY KEY AUTO_INCREMENT,
  fdoctype VARCHAR(40) NOT NULL COMMENT 'AP/AP_ESTIMATE/AP_PAYMENT_APPLY/AP_PAYMENT_PROCESS/AR/AR_ESTIMATE/AR_SETTLEMENT',
  fnumber VARCHAR(64) NOT NULL,
  fdate DATE NOT NULL,
  fcounterparty VARCHAR(128) NOT NULL,
  famount DECIMAL(18,2) NOT NULL,
  fstatus VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  fremark VARCHAR(500) NULL,
  faudited_by VARCHAR(64) NULL,
  faudited_time DATETIME NULL,
  KEY idx_arap_type (fdoctype),
  KEY idx_arap_status (fstatus),
  KEY idx_arap_date (fdate)
) COMMENT='应收应付单据';
