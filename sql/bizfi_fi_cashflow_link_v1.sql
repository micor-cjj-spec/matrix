ALTER TABLE bizfi_fi_voucher_line
    ADD COLUMN IF NOT EXISTS fcashflow_item VARCHAR(64) NULL COMMENT '现金流项目编码';

ALTER TABLE bizfi_fi_gl_entry
    ADD COLUMN IF NOT EXISTS fcashflow_item VARCHAR(64) NULL COMMENT '现金流项目编码';

CREATE INDEX IF NOT EXISTS idx_voucher_line_cashflow_item ON bizfi_fi_voucher_line(fcashflow_item);
CREATE INDEX IF NOT EXISTS idx_gl_entry_cashflow_item ON bizfi_fi_gl_entry(fcashflow_item);
