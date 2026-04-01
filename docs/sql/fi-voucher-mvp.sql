-- 凭证MVP结构补充（MySQL 8）
-- 执行前请先备份

ALTER TABLE bizfi_fi_voucher
    ADD COLUMN IF NOT EXISTS fstatus VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态:DRAFT/SUBMITTED/AUDITED/POSTED',
    ADD COLUMN IF NOT EXISTS faudited_by VARCHAR(64) NULL COMMENT '审核人',
    ADD COLUMN IF NOT EXISTS faudited_time DATETIME NULL COMMENT '审核时间',
    ADD COLUMN IF NOT EXISTS fposted_by VARCHAR(64) NULL COMMENT '过账人',
    ADD COLUMN IF NOT EXISTS fposted_time DATETIME NULL COMMENT '过账时间',
    ADD COLUMN IF NOT EXISTS fremark VARCHAR(500) NULL COMMENT '备注';

CREATE INDEX idx_voucher_status ON bizfi_fi_voucher(fstatus);
CREATE INDEX idx_voucher_date ON bizfi_fi_voucher(fdate);
CREATE INDEX idx_voucher_number ON bizfi_fi_voucher(fnumber);
