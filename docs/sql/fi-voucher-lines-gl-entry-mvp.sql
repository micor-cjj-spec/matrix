-- 凭证明细 + 过账分录 MVP 结构（MySQL 8）
-- 执行前请先备份

CREATE TABLE IF NOT EXISTS bizfi_fi_voucher_line (
  fid BIGINT PRIMARY KEY AUTO_INCREMENT,
  fvoucher_id BIGINT NOT NULL COMMENT '凭证ID',
  fline_no INT NOT NULL COMMENT '行号',
  faccount_code VARCHAR(64) NOT NULL COMMENT '科目编码',
  fsummary VARCHAR(255) NULL COMMENT '摘要',
  fdebit_amount DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '借方金额',
  fcredit_amount DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '贷方金额',
  fcurrency VARCHAR(20) NULL COMMENT '币种',
  frate DECIMAL(18,8) NULL COMMENT '汇率',
  foriginal_amount DECIMAL(18,2) NULL COMMENT '原币金额',
  INDEX idx_voucher_line_voucher(fvoucher_id),
  INDEX idx_voucher_line_account(faccount_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='财务凭证明细';

CREATE TABLE IF NOT EXISTS bizfi_fi_gl_entry (
  fid BIGINT PRIMARY KEY AUTO_INCREMENT,
  fvoucher_id BIGINT NOT NULL COMMENT '凭证ID',
  fvoucher_line_id BIGINT NULL COMMENT '来源分录ID',
  fvoucher_number VARCHAR(64) NOT NULL COMMENT '凭证号',
  fvoucher_date DATE NOT NULL COMMENT '凭证日期',
  faccount_code VARCHAR(64) NOT NULL COMMENT '科目编码',
  fsummary VARCHAR(255) NULL COMMENT '摘要',
  fdebit_amount DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '借方金额',
  fcredit_amount DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '贷方金额',
  fposted_time DATETIME NOT NULL COMMENT '过账时间',
  fposted_by VARCHAR(64) NULL COMMENT '过账人',
  INDEX idx_gl_entry_voucher(fvoucher_id),
  INDEX idx_gl_entry_account(faccount_code),
  INDEX idx_gl_entry_date(fvoucher_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='总账过账分录';
