USE matrix_fi;

-- P0-IMP-08: BANK_PAYMENT MATCHED -> Payment Settlement Finalize -> PAYMENT_COMPLETED
-- -> PURCHASE_PAYMENT_RECOGNITION -> Voucher.

ALTER TABLE matrix_fi_payment_application_allocation
    ADD COLUMN fconsumed_amount DECIMAL(20,2) NOT NULL DEFAULT 0
        COMMENT '已被成功支付并核销的占用金额'
        AFTER freserved_amount;

ALTER TABLE matrix_fi_payment_order
    ADD COLUMN fpaid_time DATETIME NULL AFTER fpaying_time,
    ADD COLUMN fsettlement_id BIGINT NULL AFTER freconciliation_case_id;

CREATE INDEX idx_matrix_fi_payment_order_settlement
    ON matrix_fi_payment_order (ftenant_id, fsettlement_id);

CREATE TABLE IF NOT EXISTS matrix_fi_ap_settlement (
    fid BIGINT NOT NULL,
    ftenant_id VARCHAR(64) NOT NULL,
    forg_id BIGINT NOT NULL,
    fnumber VARCHAR(100) NOT NULL,
    fpayment_order_id BIGINT NOT NULL,
    fbank_transaction_id BIGINT NOT NULL,
    fbusiness_partner_id VARCHAR(100) NOT NULL,
    fbusiness_partner_code VARCHAR(100) NULL,
    fbusiness_partner_name VARCHAR(255) NULL,
    fcurrency_code VARCHAR(20) NOT NULL,
    famount DECIMAL(20,2) NOT NULL,
    fstatus VARCHAR(32) NOT NULL COMMENT 'COMPLETED/REVERSED',
    fsettlement_date DATE NOT NULL,
    fbusiness_event_id VARCHAR(100) NULL,
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
    UNIQUE KEY uk_matrix_fi_ap_settlement_number (ftenant_id, fnumber),
    UNIQUE KEY uk_matrix_fi_ap_settlement_order (
        ftenant_id, fpayment_order_id, fdelete_flag
    ),
    UNIQUE KEY uk_matrix_fi_ap_settlement_bank (
        ftenant_id, fbank_transaction_id, fdelete_flag
    ),
    KEY idx_matrix_fi_ap_settlement_status (
        ftenant_id, forg_id, fstatus, fsettlement_date
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应付付款核销';

CREATE TABLE IF NOT EXISTS matrix_fi_ap_settlement_entry (
    fid BIGINT NOT NULL,
    ftenant_id VARCHAR(64) NOT NULL,
    forg_id BIGINT NOT NULL,
    fsettlement_id BIGINT NOT NULL,
    fpayable_id BIGINT NOT NULL,
    fpayable_number VARCHAR(100) NOT NULL,
    fpayment_application_id BIGINT NOT NULL,
    fpayment_application_allocation_id BIGINT NOT NULL,
    fpayment_order_allocation_id BIGINT NOT NULL,
    fsettled_amount DECIMAL(20,2) NOT NULL,
    foriginal_open_amount DECIMAL(20,2) NOT NULL,
    fremaining_open_amount DECIMAL(20,2) NOT NULL,
    foriginal_reserved_amount DECIMAL(20,2) NOT NULL,
    fremaining_reserved_amount DECIMAL(20,2) NOT NULL,
    fstatus VARCHAR(32) NOT NULL DEFAULT 'COMPLETED',
    fcreate_by BIGINT NULL,
    fcreate_time DATETIME NOT NULL,
    fmodify_by BIGINT NULL,
    fmodify_time DATETIME NOT NULL,
    fdelete_flag TINYINT NOT NULL DEFAULT 0,
    fversion INT NOT NULL DEFAULT 0,
    PRIMARY KEY (fid),
    UNIQUE KEY uk_matrix_fi_ap_settlement_entry (
        fsettlement_id,
        fpayment_order_allocation_id,
        fpayment_application_allocation_id,
        fdelete_flag
    ),
    KEY idx_matrix_fi_ap_settlement_entry_payable (
        ftenant_id, fpayable_id, fstatus
    ),
    KEY idx_matrix_fi_ap_settlement_entry_application (
        ftenant_id, fpayment_application_id
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应付付款核销分录';

ALTER TABLE matrix_fi_accounting_trace
    ADD COLUMN fsettlement_id BIGINT NULL AFTER fpayable_id,
    ADD COLUMN fpayment_order_id BIGINT NULL AFTER fsettlement_id,
    ADD COLUMN fbank_transaction_id BIGINT NULL AFTER fpayment_order_id;

CREATE INDEX idx_matrix_fi_accounting_trace_settlement
    ON matrix_fi_accounting_trace (ftenant_id, fsettlement_id);

CREATE INDEX idx_matrix_fi_accounting_trace_payment
    ON matrix_fi_accounting_trace (
        ftenant_id, fpayment_order_id, fbank_transaction_id
    );

-- Payment accounting rule. Account mappings are deliberately not seeded.
-- Finance must configure FORMAL_AP and BANK_DEPOSIT before enabling the consumer.
INSERT INTO matrix_fi_accounting_rule
(fid, ftenant_id, forg_id, frule_code, frule_name, fevent_type, fpriority,
 fsource, fstatus, feffective_date, fexpiry_date,
 fcreate_by, fcreate_time, fmodify_by, fmodify_time, fdelete_flag, fversion)
SELECT 940080001001, NULL, NULL,
       'P0_PURCHASE_PAYMENT_RECOGNITION',
       '采购付款核算',
       'PURCHASE_PAYMENT_RECOGNITION',
       100, 'FORMAL', 'PUBLISHED', NULL, NULL,
       NULL, NOW(), NULL, NOW(), 0, 0
WHERE NOT EXISTS (
    SELECT 1 FROM matrix_fi_accounting_rule
     WHERE frule_code = 'P0_PURCHASE_PAYMENT_RECOGNITION'
       AND fdelete_flag = 0
);

SELECT fid INTO @payment_rule_id
FROM matrix_fi_accounting_rule
WHERE frule_code = 'P0_PURCHASE_PAYMENT_RECOGNITION'
  AND fdelete_flag = 0
ORDER BY fid LIMIT 1;

INSERT IGNORE INTO matrix_fi_accounting_rule_version
(fid, frule_id, fversion_no, fbook_id, fstatus, fsource,
 fdefinition_hash, fpublished_time, fcreate_by, fcreate_time, fdelete_flag)
VALUES
(940080001101, @payment_rule_id, 1, 'DEFAULT', 'PUBLISHED', 'FORMAL',
 NULL, NOW(), NULL, NOW(), 0);

SELECT fid INTO @payment_rule_version_id
FROM matrix_fi_accounting_rule_version
WHERE frule_id = @payment_rule_id
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
(940080001201, @payment_rule_version_id, 10, 'HEADER', 'DEBIT',
 'MAPPING', 'FORMAL_AP', NULL,
 'FIELD(amount)', 0,
 '采购付款-${sourceDocumentNo}', 'FIELD(currencyCode)',
 'ACTIVE', NOW(), 0),
(940080001202, @payment_rule_version_id, 20, 'HEADER', 'CREDIT',
 'MAPPING', 'BANK_DEPOSIT', NULL,
 'FIELD(amount)', 0,
 '采购付款-${sourceDocumentNo}', 'FIELD(currencyCode)',
 'ACTIVE', NOW(), 0);

SELECT fid INTO @payment_ap_entry_id
FROM matrix_fi_accounting_rule_entry
WHERE frule_version_id = @payment_rule_version_id
  AND fline_no = 10
  AND fdelete_flag = 0
ORDER BY fid LIMIT 1;

SELECT fid INTO @payment_bank_entry_id
FROM matrix_fi_accounting_rule_entry
WHERE frule_version_id = @payment_rule_version_id
  AND fline_no = 20
  AND fdelete_flag = 0
ORDER BY fid LIMIT 1;

INSERT IGNORE INTO matrix_fi_accounting_rule_dimension
(fid, frule_version_id, frule_entry_id, fdimension_code,
 fsource_path, frequired, fstatus, fcreate_time, fdelete_flag)
VALUES
(940080001301, @payment_rule_version_id, @payment_ap_entry_id,
 'BUSINESS_PARTNER', 'payload.businessPartnerId', 1, 'ACTIVE', NOW(), 0),
(940080001302, @payment_rule_version_id, @payment_bank_entry_id,
 'BANK_ACCOUNT', 'payload.payerBankAccountId', 1, 'ACTIVE', NOW(), 0);
