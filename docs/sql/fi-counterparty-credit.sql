CREATE TABLE IF NOT EXISTS bizfi_fi_counterparty_credit (
  fid BIGINT PRIMARY KEY AUTO_INCREMENT,
  fcounterparty VARCHAR(128) NOT NULL,
  fdoc_type_root VARCHAR(8) NOT NULL COMMENT 'AR/AP',
  fcredit_limit DECIMAL(18,2) NOT NULL,
  foverdue_days_threshold INT NOT NULL DEFAULT 30,
  fenabled TINYINT NOT NULL DEFAULT 1,
  fblock_on_over_limit TINYINT NOT NULL DEFAULT 0,
  fblock_on_overdue TINYINT NOT NULL DEFAULT 0,
  fremark VARCHAR(500) NULL,
  fupdated_by VARCHAR(64) NULL,
  fupdated_time DATETIME NULL,
  UNIQUE KEY uk_counterparty_root (fcounterparty, fdoc_type_root),
  KEY idx_root_enabled (fdoc_type_root, fenabled)
) COMMENT='往来方信用控制配置';
