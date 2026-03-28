-- Finance foundation phase-1 schema (MySQL 8)
-- Back up data before running in production.

CREATE TABLE IF NOT EXISTS bizfi_fi_accounting_period (
    fid BIGINT PRIMARY KEY AUTO_INCREMENT,
    forg BIGINT NOT NULL COMMENT '组织ID',
    fperiod CHAR(7) NOT NULL COMMENT '会计期间yyyy-MM',
    fyear INT NOT NULL COMMENT '年度',
    fmonth INT NOT NULL COMMENT '月度',
    fstart_date DATE NOT NULL COMMENT '开始日期',
    fend_date DATE NOT NULL COMMENT '结束日期',
    fstatus VARCHAR(20) NOT NULL DEFAULT 'OPEN' COMMENT '状态:OPEN/CLOSED',
    fclose_by VARCHAR(64) NULL COMMENT '关账人',
    fclose_time DATETIME NULL COMMENT '关账时间',
    fremark VARCHAR(500) NULL COMMENT '备注',
    fcreatetime DATETIME NOT NULL COMMENT '创建时间',
    fupdatetime DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_fi_period_org_period (forg, fperiod),
    KEY idx_fi_period_org_status (forg, fstatus),
    KEY idx_fi_period_year_month (fyear, fmonth)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会计期间';

CREATE TABLE IF NOT EXISTS bizfi_fi_org_finance_config (
    fid BIGINT PRIMARY KEY AUTO_INCREMENT,
    forg BIGINT NOT NULL COMMENT '组织ID',
    fbase_currency VARCHAR(16) NOT NULL DEFAULT 'CNY' COMMENT '本位币',
    fcurrent_period CHAR(7) NULL COMMENT '当前期间yyyy-MM',
    fperiod_control_mode VARCHAR(20) NOT NULL DEFAULT 'STRICT' COMMENT '期间控制模式:STRICT/FLEXIBLE',
    fdefault_voucher_type VARCHAR(32) NULL COMMENT '默认凭证字编码',
    fstatus VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '状态:ENABLED/DISABLED',
    fremark VARCHAR(500) NULL COMMENT '备注',
    fcreatetime DATETIME NOT NULL COMMENT '创建时间',
    fupdatetime DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_fi_org_config_org (forg),
    KEY idx_fi_org_config_status (fstatus)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组织财务配置';

CREATE TABLE IF NOT EXISTS bizfi_fi_voucher_type (
    fid BIGINT PRIMARY KEY AUTO_INCREMENT,
    forg BIGINT NOT NULL COMMENT '组织ID',
    fcode VARCHAR(32) NOT NULL COMMENT '凭证字编码',
    fname VARCHAR(64) NOT NULL COMMENT '凭证字名称',
    fprefix VARCHAR(32) NULL COMMENT '编号前缀',
    fsort INT NOT NULL DEFAULT 0 COMMENT '排序',
    fstatus VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '状态:ENABLED/DISABLED',
    fremark VARCHAR(500) NULL COMMENT '备注',
    fcreatetime DATETIME NOT NULL COMMENT '创建时间',
    fupdatetime DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_fi_voucher_type_org_code (forg, fcode),
    KEY idx_fi_voucher_type_org_status (forg, fstatus, fsort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='凭证字';
