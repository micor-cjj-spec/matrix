-- Voucher lines + posted GL entries (MySQL 8)
-- Back up data before running in production.

CREATE TABLE IF NOT EXISTS bizfi_fi_voucher_line (
    fid BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary key',
    fvoucher_id BIGINT NOT NULL COMMENT 'Voucher ID',
    fline_no INT NOT NULL COMMENT 'Line number',
    faccount_code VARCHAR(64) NOT NULL COMMENT 'Account code',
    fsummary VARCHAR(255) NULL COMMENT 'Line summary',
    fdebit_amount DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT 'Debit amount',
    fcredit_amount DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT 'Credit amount',
    fcurrency VARCHAR(16) NULL COMMENT 'Currency code',
    frate DECIMAL(18,6) NULL COMMENT 'Exchange rate',
    foriginal_amount DECIMAL(18,2) NULL COMMENT 'Original currency amount',
    fcashflow_item VARCHAR(64) NULL COMMENT 'Cash-flow item code'
) COMMENT='Voucher lines';

CREATE INDEX idx_voucher_line_vid ON bizfi_fi_voucher_line(fvoucher_id);
CREATE INDEX idx_voucher_line_account ON bizfi_fi_voucher_line(faccount_code);
CREATE INDEX idx_voucher_line_cashflow ON bizfi_fi_voucher_line(fcashflow_item);

CREATE TABLE IF NOT EXISTS bizfi_fi_gl_entry (
    fid BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary key',
    fvoucher_id BIGINT NOT NULL COMMENT 'Voucher ID',
    fvoucher_line_id BIGINT NULL COMMENT 'Voucher line ID',
    fvoucher_number VARCHAR(64) NOT NULL COMMENT 'Voucher number',
    fvoucher_date DATE NOT NULL COMMENT 'Voucher date',
    faccount_code VARCHAR(64) NOT NULL COMMENT 'Account code',
    fsummary VARCHAR(255) NULL COMMENT 'Summary',
    fdebit_amount DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT 'Debit amount',
    fcredit_amount DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT 'Credit amount',
    fcashflow_item VARCHAR(64) NULL COMMENT 'Cash-flow item code',
    fposted_time DATETIME NOT NULL COMMENT 'Posted time',
    fposted_by VARCHAR(64) NULL COMMENT 'Posted by'
) COMMENT='Posted GL entries';

CREATE INDEX idx_gl_entry_vid ON bizfi_fi_gl_entry(fvoucher_id);
CREATE INDEX idx_gl_entry_date ON bizfi_fi_gl_entry(fvoucher_date);
CREATE INDEX idx_gl_entry_account ON bizfi_fi_gl_entry(faccount_code);
CREATE INDEX idx_gl_entry_cashflow ON bizfi_fi_gl_entry(fcashflow_item);
