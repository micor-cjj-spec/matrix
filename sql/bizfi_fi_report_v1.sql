CREATE TABLE IF NOT EXISTS bizfi_fi_report_template (
    fid BIGINT AUTO_INCREMENT PRIMARY KEY,
    fcode VARCHAR(64) NOT NULL,
    fname VARCHAR(100) NOT NULL,
    ftype VARCHAR(32) NOT NULL,
    forg BIGINT NULL,
    fversion VARCHAR(32) NOT NULL DEFAULT 'V1',
    fstatus VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    fremark VARCHAR(500) NULL,
    fcreatetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fupdatetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_fcode_version (fcode, fversion, forg),
    KEY idx_ftype (ftype),
    KEY idx_fstatus (fstatus)
) COMMENT='Report template';

CREATE TABLE IF NOT EXISTS bizfi_fi_report_item (
    fid BIGINT AUTO_INCREMENT PRIMARY KEY,
    ftemplate_id BIGINT NOT NULL,
    fparent_id BIGINT NULL,
    fcode VARCHAR(64) NOT NULL,
    fname VARCHAR(200) NOT NULL,
    frow_no VARCHAR(20) NULL,
    flevel INT NOT NULL DEFAULT 1,
    fline_type VARCHAR(20) NOT NULL DEFAULT 'DETAIL',
    fperiod_mode VARCHAR(20) NOT NULL DEFAULT 'END',
    fsign_rule VARCHAR(20) NOT NULL DEFAULT 'RAW',
    fdrillable TINYINT(1) NOT NULL DEFAULT 1,
    feditable_adjustment TINYINT(1) NOT NULL DEFAULT 0,
    fsort INT NOT NULL DEFAULT 0,
    fremark VARCHAR(500) NULL,
    fcreatetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fupdatetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_ftemplate_sort (ftemplate_id, fsort),
    KEY idx_fparent (fparent_id)
) COMMENT='Report item';

CREATE TABLE IF NOT EXISTS bizfi_fi_report_rule (
    fid BIGINT AUTO_INCREMENT PRIMARY KEY,
    fitem_id BIGINT NOT NULL,
    frule_type VARCHAR(32) NOT NULL,
    frule_expr TEXT NULL,
    fsign_transform VARCHAR(20) NULL,
    fpriority INT NOT NULL DEFAULT 0,
    fstatus VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    fcreatetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fupdatetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_fitem (fitem_id),
    KEY idx_frule_type (frule_type)
) COMMENT='Report rule';

CREATE TABLE IF NOT EXISTS bizfi_fi_report_account_map (
    fid BIGINT AUTO_INCREMENT PRIMARY KEY,
    ftemplate_id BIGINT NOT NULL,
    fitem_id BIGINT NOT NULL,
    faccount_id BIGINT NOT NULL,
    fmapping_type VARCHAR(20) NOT NULL DEFAULT 'DIRECT',
    feffective_from VARCHAR(20) NULL,
    feffective_to VARCHAR(20) NULL,
    fremark VARCHAR(500) NULL,
    fcreatetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fupdatetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_ftemplate_item (ftemplate_id, fitem_id),
    KEY idx_faccount (faccount_id)
) COMMENT='Report account map';

CREATE TABLE IF NOT EXISTS bizfi_fi_cashflow_item (
    fid BIGINT AUTO_INCREMENT PRIMARY KEY,
    fcode VARCHAR(64) NOT NULL,
    fname VARCHAR(200) NOT NULL,
    fparent_id BIGINT NULL,
    fcategory VARCHAR(20) NOT NULL,
    fdirection VARCHAR(20) NOT NULL,
    fsort INT NOT NULL DEFAULT 0,
    fstatus VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    fremark VARCHAR(500) NULL,
    fcreatetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fupdatetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_fcategory (fcategory),
    KEY idx_fparent (fparent_id)
) COMMENT='Cashflow item';

INSERT INTO bizfi_fi_report_template
    (fid, fcode, fname, ftype, forg, fversion, fstatus, fremark)
VALUES
    (1001, 'BS-STD', '资产负债表（标准版）', 'BALANCE_SHEET', 1, 'V1', 'ENABLED', 'seed data'),
    (1002, 'PL-STD', '利润表（标准版）', 'PROFIT_STATEMENT', 1, 'V1', 'ENABLED', 'seed data'),
    (1003, 'CF-STD', '现金流量表（标准版）', 'CASH_FLOW', 1, 'V1', 'ENABLED', 'seed data')
ON DUPLICATE KEY UPDATE
    fname = VALUES(fname),
    fstatus = VALUES(fstatus),
    fupdatetime = CURRENT_TIMESTAMP;

INSERT INTO bizfi_fi_report_item
    (fid, ftemplate_id, fparent_id, fcode, fname, frow_no, flevel, fline_type, fperiod_mode, fsign_rule, fdrillable, feditable_adjustment, fsort, fremark)
VALUES
    (1101, 1001, NULL, 'BS_ASSET', '资产', '1', 1, 'TOTAL', 'END', 'RAW', 1, 0, 10, 'seed data'),
    (1102, 1001, 1101, 'BS_CASH', '货币资金', '2', 2, 'DETAIL', 'END', 'RAW', 1, 0, 20, 'seed data'),
    (1103, 1001, NULL, 'BS_LIAB_EQ', '负债和所有者权益', '3', 1, 'TOTAL', 'END', 'RAW', 1, 0, 30, 'seed data'),
    (1201, 1002, NULL, 'PL_REVENUE', '营业收入', '1', 1, 'DETAIL', 'CURRENT', 'RAW', 1, 0, 10, 'seed data'),
    (1202, 1002, NULL, 'PL_COST', '营业成本', '2', 1, 'DETAIL', 'CURRENT', 'RAW', 1, 0, 20, 'seed data'),
    (1203, 1002, NULL, 'PL_NET_PROFIT', '净利润', '3', 1, 'FORMULA', 'CURRENT', 'RAW', 1, 0, 30, 'seed data'),
    (1301, 1003, NULL, 'CF_OPERATING', '经营活动现金流量净额', '1', 1, 'DETAIL', 'CURRENT', 'RAW', 1, 0, 10, 'seed data'),
    (1302, 1003, NULL, 'CF_INVESTING', '投资活动现金流量净额', '2', 1, 'DETAIL', 'CURRENT', 'RAW', 1, 0, 20, 'seed data'),
    (1303, 1003, NULL, 'CF_FINANCING', '筹资活动现金流量净额', '3', 1, 'DETAIL', 'CURRENT', 'RAW', 1, 0, 30, 'seed data'),
    (1304, 1003, NULL, 'CF_NET_INCREASE', '现金及现金等价物净增加额', '4', 1, 'FORMULA', 'CURRENT', 'RAW', 1, 0, 40, 'seed data')
ON DUPLICATE KEY UPDATE
    fname = VALUES(fname),
    fsort = VALUES(fsort),
    fupdatetime = CURRENT_TIMESTAMP;

INSERT INTO bizfi_fi_cashflow_item
    (fid, fcode, fname, fparent_id, fcategory, fdirection, fsort, fstatus, fremark)
VALUES
    (2001, 'CF_OPERATING', '经营活动现金流项目', NULL, 'OPERATING', 'BOTH', 10, 'ENABLED', 'seed data'),
    (2002, 'CF_INVESTING', '投资活动现金流项目', NULL, 'INVESTING', 'BOTH', 20, 'ENABLED', 'seed data'),
    (2003, 'CF_FINANCING', '筹资活动现金流项目', NULL, 'FINANCING', 'BOTH', 30, 'ENABLED', 'seed data')
ON DUPLICATE KEY UPDATE
    fname = VALUES(fname),
    fcategory = VALUES(fcategory),
    fdirection = VALUES(fdirection),
    fsort = VALUES(fsort),
    fstatus = VALUES(fstatus),
    fupdatetime = CURRENT_TIMESTAMP;
