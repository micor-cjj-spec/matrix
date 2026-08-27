USE matrix_fi;

ALTER TABLE matrix_fi_ap_payable
    ADD COLUMN fdeducted_amount DECIMAL(20,2) NOT NULL DEFAULT 0
        COMMENT '供应商索赔/采购扣款累计冲减金额'
        AFTER fsettled_amount;

CREATE TABLE IF NOT EXISTS matrix_fi_ap_deduction (
    fid BIGINT NOT NULL,
    ftenant_id VARCHAR(64) NOT NULL,
    forg_id BIGINT NOT NULL,
    fbusiness_event_id VARCHAR(100) NOT NULL,
    ferp_deduction_id VARCHAR(100) NOT NULL,
    ferp_deduction_no VARCHAR(100) NULL,
    fsupplier_claim_id VARCHAR(100) NULL,
    fpurchase_order_id VARCHAR(100) NOT NULL,
    fbusiness_partner_id VARCHAR(100) NOT NULL,
    fbusiness_partner_code VARCHAR(100) NULL,
    fbusiness_partner_name VARCHAR(255) NULL,
    fcurrency_code VARCHAR(20) NOT NULL,
    famount DECIMAL(20,2) NOT NULL,
    fstatus VARCHAR(32) NOT NULL COMMENT 'PROCESSING/COMPLETED/FAILED',
    faccounting_event_id VARCHAR(100) NULL,
    fvoucher_id BIGINT NULL,
    fvoucher_number VARCHAR(100) NULL,
    fcreate_by BIGINT NULL,
    fcreate_time DATETIME NOT NULL,
    fmodify_by BIGINT NULL,
    fmodify_time DATETIME NOT NULL,
    fdelete_flag TINYINT NOT NULL DEFAULT 0,
    fversion INT NOT NULL DEFAULT 0,
    PRIMARY KEY (fid),
    UNIQUE KEY uk_matrix_fi_ap_deduction_event (ftenant_id, fbusiness_event_id, fdelete_flag),
    UNIQUE KEY uk_matrix_fi_ap_deduction_source (ftenant_id, ferp_deduction_id, fdelete_flag),
    KEY idx_matrix_fi_ap_deduction_order (ftenant_id, fpurchase_order_id),
    KEY idx_matrix_fi_ap_deduction_status (ftenant_id, forg_id, fstatus)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP采购扣款形成的应付冲减事实';

CREATE TABLE IF NOT EXISTS matrix_fi_ap_deduction_allocation (
    fid BIGINT NOT NULL,
    ftenant_id VARCHAR(64) NOT NULL,
    forg_id BIGINT NOT NULL,
    fdeduction_id BIGINT NOT NULL,
    fbusiness_event_id VARCHAR(100) NOT NULL,
    ferp_deduction_entry_id VARCHAR(100) NOT NULL,
    fpurchase_order_entry_id VARCHAR(100) NOT NULL,
    fpayable_id BIGINT NOT NULL,
    fpayable_entry_id BIGINT NOT NULL,
    famount DECIMAL(20,2) NOT NULL,
    foriginal_open_amount DECIMAL(20,2) NOT NULL,
    fremaining_open_amount DECIMAL(20,2) NOT NULL,
    fcreate_by BIGINT NULL,
    fcreate_time DATETIME NOT NULL,
    fdelete_flag TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (fid),
    UNIQUE KEY uk_matrix_fi_ap_deduction_alloc (
        fdeduction_id, ferp_deduction_entry_id, fpayable_entry_id, fdelete_flag
    ),
    KEY idx_matrix_fi_ap_deduction_alloc_payable (ftenant_id, fpayable_id),
    KEY idx_matrix_fi_ap_deduction_alloc_po_entry (ftenant_id, fpurchase_order_entry_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购扣款到正式应付分录的FIFO分配';

ALTER TABLE matrix_fi_accounting_trace
    ADD COLUMN fdeduction_id BIGINT NULL AFTER fpayable_id;

CREATE INDEX idx_matrix_fi_accounting_trace_deduction
    ON matrix_fi_accounting_trace (ftenant_id, fdeduction_id);

INSERT INTO matrix_fi_accounting_rule
(fid, ftenant_id, forg_id, frule_code, frule_name, fevent_type, fpriority,
 fsource, fstatus, feffective_date, fexpiry_date,
 fcreate_by, fcreate_time, fmodify_by, fmodify_time, fdelete_flag, fversion)
SELECT 940090001001, NULL, NULL,
       'P1_PURCHASE_DEDUCTION_RECOGNITION',
       '采购扣款核算',
       'PURCHASE_DEDUCTION_RECOGNITION',
       100, 'FORMAL', 'PUBLISHED', NULL, NULL,
       NULL, NOW(), NULL, NOW(), 0, 0
WHERE NOT EXISTS (
    SELECT 1 FROM matrix_fi_accounting_rule
     WHERE frule_code = 'P1_PURCHASE_DEDUCTION_RECOGNITION'
       AND fdelete_flag = 0
);

SELECT fid INTO @deduction_rule_id
FROM matrix_fi_accounting_rule
WHERE frule_code = 'P1_PURCHASE_DEDUCTION_RECOGNITION'
  AND fdelete_flag = 0
ORDER BY fid LIMIT 1;

INSERT IGNORE INTO matrix_fi_accounting_rule_version
(fid, frule_id, fversion_no, fbook_id, fstatus, fsource,
 fdefinition_hash, fpublished_time, fcreate_by, fcreate_time, fdelete_flag)
VALUES
(940090001101, @deduction_rule_id, 1, 'DEFAULT', 'PUBLISHED', 'FORMAL',
 NULL, NOW(), NULL, NOW(), 0);

SELECT fid INTO @deduction_rule_version_id
FROM matrix_fi_accounting_rule_version
WHERE frule_id = @deduction_rule_id
  AND fversion_no = 1
  AND fdelete_flag = 0
ORDER BY fid LIMIT 1;

INSERT IGNORE INTO matrix_fi_accounting_rule_entry
(fid, frule_version_id, fline_no, fscope, fdirection,
 faccount_source_type, faccount_key, faccount_code,
 famount_expression, fskip_zero_amount,
 fsummary_template, fcurrency_expression,
 fstatus, fcreate_time, fdelete_flag)
VALUES
(940090001201, @deduction_rule_version_id, 10, 'HEADER', 'DEBIT',
 'MAPPING', 'FORMAL_AP', NULL,
 'FIELD(amount)', 0,
 '采购扣款-${sourceDocumentNo}', 'FIELD(currencyCode)',
 'ACTIVE', NOW(), 0),
(940090001202, @deduction_rule_version_id, 20, 'HEADER', 'CREDIT',
 'MAPPING', 'PURCHASE_CLAIM_RECOVERY', NULL,
 'FIELD(amount)', 0,
 '采购扣款-${sourceDocumentNo}', 'FIELD(currencyCode)',
 'ACTIVE', NOW(), 0);

SELECT fid INTO @deduction_ap_entry_id
FROM matrix_fi_accounting_rule_entry
WHERE frule_version_id = @deduction_rule_version_id
  AND fline_no = 10 AND fdelete_flag = 0
ORDER BY fid LIMIT 1;

SELECT fid INTO @deduction_recovery_entry_id
FROM matrix_fi_accounting_rule_entry
WHERE frule_version_id = @deduction_rule_version_id
  AND fline_no = 20 AND fdelete_flag = 0
ORDER BY fid LIMIT 1;

INSERT IGNORE INTO matrix_fi_accounting_rule_dimension
(fid, frule_version_id, frule_entry_id, fdimension_code,
 fsource_path, frequired, fstatus, fcreate_time, fdelete_flag)
VALUES
(940090001301, @deduction_rule_version_id, @deduction_ap_entry_id,
 'BUSINESS_PARTNER', 'payload.businessPartnerId', 1, 'ACTIVE', NOW(), 0),
(940090001302, @deduction_rule_version_id, @deduction_recovery_entry_id,
 'BUSINESS_PARTNER', 'payload.businessPartnerId', 1, 'ACTIVE', NOW(), 0);

-- Do not seed PURCHASE_CLAIM_RECOVERY account mapping.
-- Finance must configure it before enabling the deduction accounting consumer.
