USE matrix_fi;

-- P0-IMP-05: SupplierInvoice -> full estimate reversal -> residual estimate -> Formal AP.
-- Run once after deliverables/fi/003-inbound-accounting/schema.sql.

ALTER TABLE matrix_fi_accounting_rule_entry
    ADD COLUMN fskip_zero_amount TINYINT NOT NULL DEFAULT 0
        COMMENT '1=金额为0时跳过该规则分录；0=金额必须大于0'
        AFTER famount_expression;

ALTER TABLE matrix_fi_ap_payable
    ADD COLUMN foriginal_payable_id BIGINT NULL
        COMMENT '残余暂估来源的原暂估应付ID'
        AFTER fbusiness_event_id;

ALTER TABLE matrix_fi_ap_payable_entry
    ADD COLUMN fnet_amount DECIMAL(20,2) NULL AFTER famount,
    ADD COLUMN ftax_rate DECIMAL(20,6) NULL AFTER fnet_amount,
    ADD COLUMN ftax_amount DECIMAL(20,2) NULL AFTER ftax_rate,
    ADD COLUMN fgross_amount DECIMAL(20,2) NULL AFTER ftax_amount;

CREATE TABLE IF NOT EXISTS matrix_fi_ap_estimate_reversal (
    fid BIGINT NOT NULL,
    ftenant_id VARCHAR(64) NOT NULL,
    forg_id BIGINT NOT NULL,
    fbusiness_event_id VARCHAR(100) NOT NULL,
    fsupplier_invoice_id VARCHAR(100) NOT NULL,
    fsupplier_invoice_no VARCHAR(100) NULL,
    festimate_payable_id BIGINT NOT NULL,
    fformal_payable_id BIGINT NOT NULL,
    freversal_amount DECIMAL(20,2) NOT NULL,
    fresidual_payable_id BIGINT NULL,
    fresidual_amount DECIMAL(20,2) NOT NULL DEFAULT 0,
    freversal_accounting_event_id VARCHAR(100) NULL,
    fresidual_accounting_event_id VARCHAR(100) NULL,
    fstatus VARCHAR(32) NOT NULL COMMENT 'PROCESSING/COMPLETED/FAILED',
    fcreate_by BIGINT NULL,
    fcreate_time DATETIME NOT NULL,
    fmodify_by BIGINT NULL,
    fmodify_time DATETIME NOT NULL,
    fdelete_flag TINYINT NOT NULL DEFAULT 0,
    fversion INT NOT NULL DEFAULT 0,
    PRIMARY KEY (fid),
    UNIQUE KEY uk_matrix_fi_ap_estimate_reversal (
        ftenant_id, fbusiness_event_id, festimate_payable_id, fdelete_flag
    ),
    KEY idx_matrix_fi_ap_estimate_reversal_estimate (ftenant_id, festimate_payable_id),
    KEY idx_matrix_fi_ap_estimate_reversal_invoice (ftenant_id, fsupplier_invoice_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应商发票触发的原暂估全额冲回记录';

CREATE TABLE IF NOT EXISTS matrix_fi_ap_estimate_reversal_allocation (
    fid BIGINT NOT NULL,
    ftenant_id VARCHAR(64) NOT NULL,
    forg_id BIGINT NOT NULL,
    freversal_id BIGINT NOT NULL,
    fbusiness_event_id VARCHAR(100) NOT NULL,
    fsupplier_invoice_entry_id VARCHAR(100) NOT NULL,
    festimate_payable_entry_id BIGINT NOT NULL,
    fmatched_quantity DECIMAL(20,6) NOT NULL,
    fmatched_amount DECIMAL(20,2) NOT NULL,
    fcreate_by BIGINT NULL,
    fcreate_time DATETIME NOT NULL,
    fdelete_flag TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (fid),
    UNIQUE KEY uk_matrix_fi_ap_estimate_reversal_alloc (
        freversal_id, fsupplier_invoice_entry_id, festimate_payable_entry_id, fdelete_flag
    ),
    KEY idx_matrix_fi_ap_estimate_reversal_alloc_invoice (
        ftenant_id, fbusiness_event_id, fsupplier_invoice_entry_id
    ),
    KEY idx_matrix_fi_ap_estimate_reversal_alloc_estimate (
        festimate_payable_entry_id
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='发票行到原暂估分录的数量/金额分配';

CREATE INDEX idx_matrix_fi_ap_payable_original
    ON matrix_fi_ap_payable (ftenant_id, foriginal_payable_id);
