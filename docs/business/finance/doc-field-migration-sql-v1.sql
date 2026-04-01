-- 财务单据字段增强 SQL（V1 草案）
-- 说明：以下以 AR/AP 头表为例，按实际表名调整执行。

-- ========== 1) AR/AP 头表通用字段（示例） ==========
-- AR
ALTER TABLE bizfi_fi_arap_doc
    ADD COLUMN company_id BIGINT NULL COMMENT '公司/主体ID',
    ADD COLUMN book_id BIGINT NULL COMMENT '账套ID',
    ADD COLUMN period CHAR(7) NULL COMMENT '会计期间YYYY-MM',
    ADD COLUMN internal_flag TINYINT DEFAULT 0 COMMENT '是否内部交易0否1是',
    ADD COLUMN internal_company_id BIGINT NULL COMMENT '内部对手主体ID',
    ADD COLUMN dept_id BIGINT NULL COMMENT '部门ID',
    ADD COLUMN project_id BIGINT NULL COMMENT '项目ID',
    ADD COLUMN biz_unit_id BIGINT NULL COMMENT '业务单元ID',
    ADD COLUMN currency_code VARCHAR(16) DEFAULT 'CNY' COMMENT '币种',
    ADD COLUMN exchange_rate DECIMAL(18,6) DEFAULT 1.000000 COMMENT '汇率',
    ADD COLUMN amount_fc DECIMAL(18,2) NULL COMMENT '原币金额',
    ADD COLUMN tax_amount DECIMAL(18,2) NULL COMMENT '税额',
    ADD COLUMN amount_ex_tax DECIMAL(18,2) NULL COMMENT '不含税金额',
    ADD COLUMN risk_level VARCHAR(16) NULL COMMENT '风险等级',
    ADD COLUMN risk_tags JSON NULL COMMENT '风险标签',
    ADD COLUMN credit_hit_over_limit TINYINT DEFAULT 0 COMMENT '命中超限',
    ADD COLUMN credit_hit_overdue TINYINT DEFAULT 0 COMMENT '命中超期',
    ADD COLUMN credit_blocked TINYINT DEFAULT 0 COMMENT '被硬拦截',
    ADD COLUMN risk_suggestion VARCHAR(255) NULL COMMENT '建议动作',
    ADD COLUMN source_type VARCHAR(32) NULL COMMENT '来源类型',
    ADD COLUMN source_id BIGINT NULL COMMENT '来源ID',
    ADD COLUMN source_no VARCHAR(64) NULL COMMENT '来源编号',
    ADD COLUMN submitted_by VARCHAR(64) NULL COMMENT '提交人',
    ADD COLUMN submitted_time DATETIME NULL COMMENT '提交时间',
    ADD COLUMN posted_by VARCHAR(64) NULL COMMENT '过账人',
    ADD COLUMN posted_time DATETIME NULL COMMENT '过账时间';

-- ========== 2) 索引建议 ==========
CREATE INDEX idx_arap_period_status ON bizfi_fi_arap_doc(period, fstatus);
CREATE INDEX idx_arap_counterparty_date ON bizfi_fi_arap_doc(fcounterparty, fdate);
CREATE INDEX idx_arap_internal ON bizfi_fi_arap_doc(internal_flag, internal_company_id);

-- 若 doc_no 在表内叫 fnumber，可按公司/账套/类型/编号做唯一键
-- ALTER TABLE bizfi_fi_arap_doc
--     ADD UNIQUE KEY uk_arap_company_book_type_no(company_id, book_id, fdoctype, fnumber);

-- ========== 3) 分录行增强（以总账分录表为示例） ==========
ALTER TABLE bizfi_fi_voucher_line
    ADD COLUMN account_name VARCHAR(128) NULL COMMENT '科目名称快照',
    ADD COLUMN amount_fc DECIMAL(18,2) NULL COMMENT '原币金额',
    ADD COLUMN assist_customer_id BIGINT NULL COMMENT '客户辅助核算',
    ADD COLUMN assist_supplier_id BIGINT NULL COMMENT '供应商辅助核算',
    ADD COLUMN assist_dept_id BIGINT NULL COMMENT '部门辅助核算',
    ADD COLUMN assist_project_id BIGINT NULL COMMENT '项目辅助核算',
    ADD COLUMN assist_employee_id BIGINT NULL COMMENT '员工辅助核算',
    ADD COLUMN tax_code VARCHAR(32) NULL COMMENT '税码',
    ADD COLUMN tax_rate DECIMAL(8,4) NULL COMMENT '税率',
    ADD COLUMN cashflow_item VARCHAR(64) NULL COMMENT '现金流量项目',
    ADD COLUMN source_line_id BIGINT NULL COMMENT '来源行ID';

-- ========== 4) 合并调整单（建议新增） ==========
CREATE TABLE IF NOT EXISTS bizfi_fi_consolidation_adj (
    fid BIGINT PRIMARY KEY AUTO_INCREMENT,
    company_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    period CHAR(7) NOT NULL,
    doc_no VARCHAR(64) NOT NULL,
    biz_date DATE NOT NULL,
    consolidation_batch_no VARCHAR(64) NOT NULL,
    elimination_type VARCHAR(32) NOT NULL,
    investee_company_id BIGINT NULL,
    holding_ratio DECIMAL(8,4) NULL,
    nci_amount DECIMAL(18,2) NULL,
    currency_code VARCHAR(16) DEFAULT 'CNY',
    exchange_rate DECIMAL(18,6) DEFAULT 1.000000,
    amount_lc DECIMAL(18,2) NOT NULL,
    status VARCHAR(16) NOT NULL,
    consolidation_note VARCHAR(500) NULL,
    created_by VARCHAR(64) NOT NULL,
    created_time DATETIME NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    updated_time DATETIME NOT NULL,
    UNIQUE KEY uk_cons_adj(company_id, book_id, period, doc_no),
    KEY idx_cons_batch(consolidation_batch_no),
    KEY idx_cons_period_status(period, status)
) COMMENT='合并调整单';

-- ========== 5) 历史数据回填（示例） ==========
UPDATE bizfi_fi_arap_doc
SET period = DATE_FORMAT(fdate, '%Y-%m')
WHERE period IS NULL AND fdate IS NOT NULL;

UPDATE bizfi_fi_arap_doc
SET currency_code = 'CNY', exchange_rate = 1.000000
WHERE currency_code IS NULL;
