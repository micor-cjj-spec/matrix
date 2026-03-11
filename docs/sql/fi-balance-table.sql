-- 总账余额表（建议）
-- 说明：用于按期间沉淀科目余额，来源为已过账分录

CREATE TABLE IF NOT EXISTS bizfi_fi_balance (
    fid BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    fcompany_id VARCHAR(64) NOT NULL COMMENT '公司/账套',
    fperiod VARCHAR(7) NOT NULL COMMENT '会计期间 yyyy-MM',
    faccount_code VARCHAR(64) NOT NULL COMMENT '科目编码',
    faux_key VARCHAR(255) NULL COMMENT '辅助核算组合键',
    fcurrency VARCHAR(16) NOT NULL DEFAULT 'CNY' COMMENT '币种',

    fopen_debit DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '期初借方余额',
    fopen_credit DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '期初贷方余额',
    fperiod_debit DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '本期借方发生',
    fperiod_credit DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '本期贷方发生',
    fclose_debit DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '期末借方余额',
    fclose_credit DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '期末贷方余额',

    fversion BIGINT NOT NULL DEFAULT 1 COMMENT '重算版本',
    fupdated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE KEY uk_balance_dim (fcompany_id, fperiod, faccount_code, faux_key, fcurrency),
    KEY idx_balance_period (fperiod),
    KEY idx_balance_account (faccount_code),
    KEY idx_balance_update_time (fupdated_time)
) COMMENT='总账余额表';
