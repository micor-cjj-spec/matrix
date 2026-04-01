-- Voucher line + GL entry MVP schema (MySQL 8)
-- Back up data before running in production.

CREATE TABLE IF NOT EXISTS bizfi_fi_voucher_line (
  fid BIGINT PRIMARY KEY AUTO_INCREMENT,
  fvoucher_id BIGINT NOT NULL COMMENT 'Voucher ID',
  fline_no INT NOT NULL COMMENT 'Line number',
  faccount_code VARCHAR(64) NOT NULL COMMENT 'Account code',
  fsummary VARCHAR(255) NULL COMMENT 'Summary',
  fdebit_amount DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT 'Debit amount',
  fcredit_amount DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT 'Credit amount',
  fcurrency VARCHAR(20) NULL COMMENT 'Currency code',
  frate DECIMAL(18,8) NULL COMMENT 'Exchange rate',
  foriginal_amount DECIMAL(18,2) NULL COMMENT 'Original amount',
  fcashflow_item VARCHAR(64) NULL COMMENT 'Cash-flow item code',
  INDEX idx_voucher_line_voucher(fvoucher_id),
  INDEX idx_voucher_line_account(faccount_code),
  INDEX idx_voucher_line_cashflow(fcashflow_item)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Voucher lines';

CREATE TABLE IF NOT EXISTS bizfi_fi_gl_entry (
  fid BIGINT PRIMARY KEY AUTO_INCREMENT,
  fvoucher_id BIGINT NOT NULL COMMENT 'Voucher ID',
  fvoucher_line_id BIGINT NULL COMMENT 'Source voucher line ID',
  fvoucher_number VARCHAR(64) NOT NULL COMMENT 'Voucher number',
  fvoucher_date DATE NOT NULL COMMENT 'Voucher date',
  faccount_code VARCHAR(64) NOT NULL COMMENT 'Account code',
  fsummary VARCHAR(255) NULL COMMENT 'Summary',
  fdebit_amount DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT 'Debit amount',
  fcredit_amount DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT 'Credit amount',
  fcashflow_item VARCHAR(64) NULL COMMENT 'Cash-flow item code',
  fposted_time DATETIME NOT NULL COMMENT 'Posted time',
  fposted_by VARCHAR(64) NULL COMMENT 'Posted by',
  INDEX idx_gl_entry_voucher(fvoucher_id),
  INDEX idx_gl_entry_account(faccount_code),
  INDEX idx_gl_entry_date(fvoucher_date),
  INDEX idx_gl_entry_cashflow(fcashflow_item)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Posted GL entries';
