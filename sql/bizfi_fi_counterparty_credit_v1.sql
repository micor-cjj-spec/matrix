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
);

DELIMITER $$

DROP PROCEDURE IF EXISTS bizfi_fi_add_counterparty_credit_columns$$
CREATE PROCEDURE bizfi_fi_add_counterparty_credit_columns()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'bizfi_fi_counterparty_credit'
      AND column_name = 'fblock_on_over_limit'
  ) THEN
    ALTER TABLE bizfi_fi_counterparty_credit
      ADD COLUMN fblock_on_over_limit TINYINT NOT NULL DEFAULT 0 AFTER fenabled;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'bizfi_fi_counterparty_credit'
      AND column_name = 'fblock_on_overdue'
  ) THEN
    ALTER TABLE bizfi_fi_counterparty_credit
      ADD COLUMN fblock_on_overdue TINYINT NOT NULL DEFAULT 0 AFTER fblock_on_over_limit;
  END IF;
END$$

CALL bizfi_fi_add_counterparty_credit_columns()$$
DROP PROCEDURE IF EXISTS bizfi_fi_add_counterparty_credit_columns$$

DELIMITER ;
