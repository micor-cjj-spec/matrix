CREATE TABLE IF NOT EXISTS bizfi_fi_voucher_line (
  fid BIGINT PRIMARY KEY AUTO_INCREMENT,
  fvoucher_id BIGINT NULL COMMENT 'Voucher ID',
  fline_no INT NULL COMMENT 'Line number',
  faccount_code VARCHAR(64) NULL COMMENT 'Account code',
  fsummary VARCHAR(255) NULL COMMENT 'Line summary',
  fdebit_amount DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT 'Debit amount',
  fcredit_amount DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT 'Credit amount',
  fcurrency VARCHAR(16) NULL COMMENT 'Currency code',
  frate DECIMAL(18,6) NULL COMMENT 'Exchange rate',
  foriginal_amount DECIMAL(18,2) NULL COMMENT 'Original currency amount',
  fcashflow_item VARCHAR(64) NULL COMMENT 'Cash-flow item code'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Voucher lines';

CREATE TABLE IF NOT EXISTS bizfi_fi_gl_entry (
  fid BIGINT PRIMARY KEY AUTO_INCREMENT,
  fvoucher_id BIGINT NULL COMMENT 'Voucher ID',
  fvoucher_line_id BIGINT NULL COMMENT 'Voucher line ID',
  fvoucher_number VARCHAR(64) NULL COMMENT 'Voucher number',
  fvoucher_date DATE NULL COMMENT 'Voucher date',
  faccount_code VARCHAR(64) NULL COMMENT 'Account code',
  fsummary VARCHAR(255) NULL COMMENT 'Summary',
  fdebit_amount DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT 'Debit amount',
  fcredit_amount DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT 'Credit amount',
  fcashflow_item VARCHAR(64) NULL COMMENT 'Cash-flow item code',
  fposted_time DATETIME NULL COMMENT 'Posted time',
  fposted_by VARCHAR(64) NULL COMMENT 'Posted by'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Posted GL entries';

DELIMITER $$

DROP PROCEDURE IF EXISTS bizfi_fi_repair_voucher_entry_schema$$
CREATE PROCEDURE bizfi_fi_repair_voucher_entry_schema()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'bizfi_fi_voucher_line'
      AND column_name = 'fvoucher_id'
  ) THEN
    ALTER TABLE bizfi_fi_voucher_line
      ADD COLUMN fvoucher_id BIGINT NULL COMMENT 'Voucher ID' AFTER fid;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'bizfi_fi_voucher_line'
      AND column_name = 'fline_no'
  ) THEN
    ALTER TABLE bizfi_fi_voucher_line
      ADD COLUMN fline_no INT NULL COMMENT 'Line number' AFTER fvoucher_id;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'bizfi_fi_voucher_line'
      AND column_name = 'faccount_code'
  ) THEN
    ALTER TABLE bizfi_fi_voucher_line
      ADD COLUMN faccount_code VARCHAR(64) NULL COMMENT 'Account code' AFTER fline_no;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'bizfi_fi_voucher_line'
      AND column_name = 'fsummary'
  ) THEN
    ALTER TABLE bizfi_fi_voucher_line
      ADD COLUMN fsummary VARCHAR(255) NULL COMMENT 'Line summary' AFTER faccount_code;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'bizfi_fi_voucher_line'
      AND column_name = 'fdebit_amount'
  ) THEN
    ALTER TABLE bizfi_fi_voucher_line
      ADD COLUMN fdebit_amount DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT 'Debit amount' AFTER fsummary;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'bizfi_fi_voucher_line'
      AND column_name = 'fcredit_amount'
  ) THEN
    ALTER TABLE bizfi_fi_voucher_line
      ADD COLUMN fcredit_amount DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT 'Credit amount' AFTER fdebit_amount;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'bizfi_fi_voucher_line'
      AND column_name = 'fcurrency'
  ) THEN
    ALTER TABLE bizfi_fi_voucher_line
      ADD COLUMN fcurrency VARCHAR(16) NULL COMMENT 'Currency code' AFTER fcredit_amount;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'bizfi_fi_voucher_line'
      AND column_name = 'frate'
  ) THEN
    ALTER TABLE bizfi_fi_voucher_line
      ADD COLUMN frate DECIMAL(18,6) NULL COMMENT 'Exchange rate' AFTER fcurrency;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'bizfi_fi_voucher_line'
      AND column_name = 'foriginal_amount'
  ) THEN
    ALTER TABLE bizfi_fi_voucher_line
      ADD COLUMN foriginal_amount DECIMAL(18,2) NULL COMMENT 'Original currency amount' AFTER frate;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'bizfi_fi_voucher_line'
      AND column_name = 'fcashflow_item'
  ) THEN
    ALTER TABLE bizfi_fi_voucher_line
      ADD COLUMN fcashflow_item VARCHAR(64) NULL COMMENT 'Cash-flow item code' AFTER foriginal_amount;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'bizfi_fi_gl_entry'
      AND column_name = 'fvoucher_id'
  ) THEN
    ALTER TABLE bizfi_fi_gl_entry
      ADD COLUMN fvoucher_id BIGINT NULL COMMENT 'Voucher ID' AFTER fid;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'bizfi_fi_gl_entry'
      AND column_name = 'fvoucher_line_id'
  ) THEN
    ALTER TABLE bizfi_fi_gl_entry
      ADD COLUMN fvoucher_line_id BIGINT NULL COMMENT 'Voucher line ID' AFTER fvoucher_id;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'bizfi_fi_gl_entry'
      AND column_name = 'fvoucher_number'
  ) THEN
    ALTER TABLE bizfi_fi_gl_entry
      ADD COLUMN fvoucher_number VARCHAR(64) NULL COMMENT 'Voucher number' AFTER fvoucher_line_id;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'bizfi_fi_gl_entry'
      AND column_name = 'fvoucher_date'
  ) THEN
    ALTER TABLE bizfi_fi_gl_entry
      ADD COLUMN fvoucher_date DATE NULL COMMENT 'Voucher date' AFTER fvoucher_number;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'bizfi_fi_gl_entry'
      AND column_name = 'faccount_code'
  ) THEN
    ALTER TABLE bizfi_fi_gl_entry
      ADD COLUMN faccount_code VARCHAR(64) NULL COMMENT 'Account code' AFTER fvoucher_date;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'bizfi_fi_gl_entry'
      AND column_name = 'fsummary'
  ) THEN
    ALTER TABLE bizfi_fi_gl_entry
      ADD COLUMN fsummary VARCHAR(255) NULL COMMENT 'Summary' AFTER faccount_code;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'bizfi_fi_gl_entry'
      AND column_name = 'fdebit_amount'
  ) THEN
    ALTER TABLE bizfi_fi_gl_entry
      ADD COLUMN fdebit_amount DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT 'Debit amount' AFTER fsummary;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'bizfi_fi_gl_entry'
      AND column_name = 'fcredit_amount'
  ) THEN
    ALTER TABLE bizfi_fi_gl_entry
      ADD COLUMN fcredit_amount DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT 'Credit amount' AFTER fdebit_amount;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'bizfi_fi_gl_entry'
      AND column_name = 'fcashflow_item'
  ) THEN
    ALTER TABLE bizfi_fi_gl_entry
      ADD COLUMN fcashflow_item VARCHAR(64) NULL COMMENT 'Cash-flow item code' AFTER fcredit_amount;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'bizfi_fi_gl_entry'
      AND column_name = 'fposted_time'
  ) THEN
    ALTER TABLE bizfi_fi_gl_entry
      ADD COLUMN fposted_time DATETIME NULL COMMENT 'Posted time' AFTER fcashflow_item;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'bizfi_fi_gl_entry'
      AND column_name = 'fposted_by'
  ) THEN
    ALTER TABLE bizfi_fi_gl_entry
      ADD COLUMN fposted_by VARCHAR(64) NULL COMMENT 'Posted by' AFTER fposted_time;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'bizfi_fi_voucher_line'
      AND index_name = 'idx_voucher_line_vid'
  ) THEN
    ALTER TABLE bizfi_fi_voucher_line ADD INDEX idx_voucher_line_vid (fvoucher_id);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'bizfi_fi_voucher_line'
      AND index_name = 'idx_voucher_line_account'
  ) THEN
    ALTER TABLE bizfi_fi_voucher_line ADD INDEX idx_voucher_line_account (faccount_code);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'bizfi_fi_voucher_line'
      AND index_name = 'idx_voucher_line_cashflow'
  ) THEN
    ALTER TABLE bizfi_fi_voucher_line ADD INDEX idx_voucher_line_cashflow (fcashflow_item);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'bizfi_fi_gl_entry'
      AND index_name = 'idx_gl_entry_vid'
  ) THEN
    ALTER TABLE bizfi_fi_gl_entry ADD INDEX idx_gl_entry_vid (fvoucher_id);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'bizfi_fi_gl_entry'
      AND index_name = 'idx_gl_entry_date'
  ) THEN
    ALTER TABLE bizfi_fi_gl_entry ADD INDEX idx_gl_entry_date (fvoucher_date);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'bizfi_fi_gl_entry'
      AND index_name = 'idx_gl_entry_account'
  ) THEN
    ALTER TABLE bizfi_fi_gl_entry ADD INDEX idx_gl_entry_account (faccount_code);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'bizfi_fi_gl_entry'
      AND index_name = 'idx_gl_entry_cashflow'
  ) THEN
    ALTER TABLE bizfi_fi_gl_entry ADD INDEX idx_gl_entry_cashflow (fcashflow_item);
  END IF;
END$$

CALL bizfi_fi_repair_voucher_entry_schema()$$
DROP PROCEDURE IF EXISTS bizfi_fi_repair_voucher_entry_schema$$

DELIMITER ;
