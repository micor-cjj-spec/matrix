USE matrix_fi;

-- P0-IMP-07: PaymentApplication -> PaymentOrder -> BankTransaction -> BANK_PAYMENT match.
-- PaymentOrder is NOT marked PAID in this migration stage.
-- P0-IMP-08 performs PAID + AP Settlement + reservation release + PAYMENT_COMPLETED atomically.

ALTER TABLE matrix_fi_payment_application
    ADD COLUMN fordered_amount DECIMAL(20,2) NOT NULL DEFAULT 0
        COMMENT '已被有效付款单占用的申请金额'
        AFTER famount;

CREATE TABLE IF NOT EXISTS matrix_fi_payment_order (
    fid BIGINT NOT NULL,
    ftenant_id VARCHAR(64) NOT NULL,
    forg_id BIGINT NOT NULL,
    fnumber VARCHAR(100) NOT NULL,
    fdate DATE NOT NULL,
    fbusiness_partner_id VARCHAR(100) NOT NULL,
    fbusiness_partner_code VARCHAR(100) NULL,
    fbusiness_partner_name VARCHAR(255) NULL,
    fcurrency_code VARCHAR(20) NOT NULL,
    famount DECIMAL(20,2) NOT NULL,
    fpayment_method VARCHAR(32) NOT NULL,
    fpayer_bank_account_id VARCHAR(100) NULL,
    fpayee_bank_account_id VARCHAR(100) NULL,
    fpayee_account_name VARCHAR(255) NULL,
    fpayee_bank_name VARCHAR(255) NULL,
    fpayee_bank_account_no VARCHAR(100) NULL,
    ffund_plan_id VARCHAR(100) NULL,
    fplanned_pay_date DATE NULL,
    fliquidity_check_status VARCHAR(32) NOT NULL DEFAULT 'PENDING'
        COMMENT 'PENDING/PASSED/FAILED/NOT_REQUIRED',
    fliquidity_check_id VARCHAR(100) NULL,
    fliquidity_available_amount DECIMAL(20,2) NULL,
    fliquidity_check_message VARCHAR(500) NULL,
    fliquidity_snapshot_json JSON NULL,
    fstatus VARCHAR(32) NOT NULL
        COMMENT 'DRAFT/SUBMITTED/AUDITED/PAYING/FAILED/REJECTED/CANCELLED/PAID',
    fapproval_status VARCHAR(32) NOT NULL
        COMMENT 'DRAFT/SUBMITTED/AUDITED/REJECTED',
    fchannel_code VARCHAR(64) NULL,
    fchannel_request_id VARCHAR(128) NULL,
    fchannel_status VARCHAR(32) NULL
        COMMENT 'NOT_SENT/SUBMITTED/FAILED/CONFIRMED',
    fchannel_error VARCHAR(1000) NULL,
    fsubmitted_time DATETIME NULL,
    faudit_by BIGINT NULL,
    faudit_time DATETIME NULL,
    fpaying_time DATETIME NULL,
    fbank_match_status VARCHAR(32) NOT NULL DEFAULT 'UNMATCHED'
        COMMENT 'UNMATCHED/MATCHED/DIFFERENCE',
    freconciliation_batch_id BIGINT NULL,
    freconciliation_case_id BIGINT NULL,
    fbotp_idempotency_key VARCHAR(160) NULL,
    fsource_system_code VARCHAR(64) NULL,
    fsource_document_type VARCHAR(100) NULL,
    fsource_document_id VARCHAR(100) NULL,
    fsource_execution_id VARCHAR(100) NULL,
    fremark VARCHAR(1000) NULL,
    freject_reason VARCHAR(1000) NULL,
    fcreate_by BIGINT NULL,
    fcreate_time DATETIME NOT NULL,
    fmodify_by BIGINT NULL,
    fmodify_time DATETIME NOT NULL,
    fdelete_flag TINYINT NOT NULL DEFAULT 0,
    fversion INT NOT NULL DEFAULT 0,
    PRIMARY KEY (fid),
    UNIQUE KEY uk_matrix_fi_payment_order_number (ftenant_id, fnumber),
    UNIQUE KEY uk_matrix_fi_payment_order_botp (
        ftenant_id, fbotp_idempotency_key, fdelete_flag
    ),
    UNIQUE KEY uk_matrix_fi_payment_order_channel_request (
        ftenant_id, fchannel_code, fchannel_request_id, fdelete_flag
    ),
    KEY idx_matrix_fi_payment_order_status (
        ftenant_id, forg_id, fstatus, fplanned_pay_date
    ),
    KEY idx_matrix_fi_payment_order_partner (
        ftenant_id, fbusiness_partner_id, fstatus
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='付款单/支付执行指令';

CREATE TABLE IF NOT EXISTS matrix_fi_payment_order_allocation (
    fid BIGINT NOT NULL,
    ftenant_id VARCHAR(64) NOT NULL,
    forg_id BIGINT NOT NULL,
    fpayment_order_id BIGINT NOT NULL,
    fpayment_application_id BIGINT NOT NULL,
    famount DECIMAL(20,2) NOT NULL,
    fstatus VARCHAR(32) NOT NULL
        COMMENT 'ORDERED/RELEASED/CONSUMED',
    fcreate_by BIGINT NULL,
    fcreate_time DATETIME NOT NULL,
    fmodify_by BIGINT NULL,
    fmodify_time DATETIME NOT NULL,
    fdelete_flag TINYINT NOT NULL DEFAULT 0,
    fversion INT NOT NULL DEFAULT 0,
    PRIMARY KEY (fid),
    UNIQUE KEY uk_matrix_fi_payment_order_alloc (
        fpayment_order_id, fpayment_application_id, fdelete_flag
    ),
    KEY idx_matrix_fi_payment_order_alloc_application (
        ftenant_id, fpayment_application_id, fstatus
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='付款单对应付款申请占用';

CREATE TABLE IF NOT EXISTS matrix_fi_bank_transaction (
    fid BIGINT NOT NULL,
    ftenant_id VARCHAR(64) NOT NULL,
    forg_id BIGINT NOT NULL,
    fbank_account_id VARCHAR(100) NOT NULL,
    fbank_transaction_no VARCHAR(128) NOT NULL,
    ftransaction_date DATE NOT NULL,
    ftransaction_time DATETIME NULL,
    fdirection VARCHAR(16) NOT NULL COMMENT 'OUTBOUND/INBOUND',
    fcurrency_code VARCHAR(20) NOT NULL,
    famount DECIMAL(20,2) NOT NULL,
    fcounterparty_name VARCHAR(255) NULL,
    fcounterparty_account VARCHAR(100) NULL,
    fpurpose VARCHAR(500) NULL,
    fsummary VARCHAR(500) NULL,
    fbank_receipt_no VARCHAR(128) NULL,
    fsource_channel VARCHAR(64) NOT NULL,
    fmatch_status VARCHAR(32) NOT NULL DEFAULT 'UNMATCHED'
        COMMENT 'UNMATCHED/MATCHED/DIFFERENCE',
    fstatus VARCHAR(32) NOT NULL DEFAULT 'CONFIRMED'
        COMMENT 'CONFIRMED/REVERSED',
    fmatched_payment_order_id BIGINT NULL,
    freconciliation_batch_id BIGINT NULL,
    freconciliation_case_id BIGINT NULL,
    fraw_payload_hash CHAR(64) NULL,
    fraw_payload_json JSON NULL,
    fcreate_by BIGINT NULL,
    fcreate_time DATETIME NOT NULL,
    fmodify_by BIGINT NULL,
    fmodify_time DATETIME NOT NULL,
    fdelete_flag TINYINT NOT NULL DEFAULT 0,
    fversion INT NOT NULL DEFAULT 0,
    PRIMARY KEY (fid),
    UNIQUE KEY uk_matrix_fi_bank_transaction (
        ftenant_id, fbank_account_id, fbank_transaction_no, fdelete_flag
    ),
    KEY idx_matrix_fi_bank_transaction_match (
        ftenant_id, forg_id, fmatch_status, ftransaction_date
    ),
    KEY idx_matrix_fi_bank_transaction_order (
        ftenant_id, fmatched_payment_order_id
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='银行流水/回单事实';

-- BANK_PAYMENT reconciliation rule v1.
INSERT INTO matrix_fi_reconciliation_rule
(fid, ftenant_id, fcode, fname, fscenario_type, fstatus, fcurrent_version, fpriority,
 fcreate_time, fmodify_time, fdelete_flag, fversion)
SELECT 940070001001, NULL, 'BANK_PAYMENT_MATCH', '付款单-银行流水匹配',
       'BANK_PAYMENT', 'PUBLISHED', 1, 100, NOW(), NOW(), 0, 0
WHERE NOT EXISTS (
    SELECT 1 FROM matrix_fi_reconciliation_rule
     WHERE ftenant_id IS NULL AND fcode = 'BANK_PAYMENT_MATCH' AND fdelete_flag = 0
);

SELECT fid INTO @bank_payment_rule_id
FROM matrix_fi_reconciliation_rule
WHERE ftenant_id IS NULL AND fcode = 'BANK_PAYMENT_MATCH' AND fdelete_flag = 0
ORDER BY fid LIMIT 1;

INSERT IGNORE INTO matrix_fi_reconciliation_rule_version
(fid, ftenant_id, frule_id, fversion_no, fstatus, fdefinition_json, fdefinition_hash,
 fpublished_time, fcreate_time, fdelete_flag)
VALUES
(940070001101, NULL, @bank_payment_rule_id, 1, 'PUBLISHED',
 JSON_OBJECT(
   'amountTolerance','ZERO',
   'requiredDirection','OUTBOUND',
   'blockingDifferences',true
 ),
 NULL, NOW(), NOW(), 0);

SELECT fid INTO @bank_payment_rule_version_id
FROM matrix_fi_reconciliation_rule_version
WHERE frule_id = @bank_payment_rule_id AND fversion_no = 1 AND fdelete_flag = 0
ORDER BY fid LIMIT 1;

INSERT IGNORE INTO matrix_fi_reconciliation_rule_field
(fid, ftenant_id, frule_version_id, ffield_code, ffield_role, fcompare_operator,
 ftolerance_type, ftolerance_value, fseverity, frequired, fsort_no, fcreate_time, fdelete_flag)
VALUES
(940070001201, NULL, @bank_payment_rule_version_id, 'DIRECTION', 'MATCH_KEY', 'EQ', 'ABSOLUTE', 0, 'BLOCKING', 1, 10, NOW(), 0),
(940070001202, NULL, @bank_payment_rule_version_id, 'BANK_ACCOUNT', 'MATCH_KEY', 'EQ', 'ABSOLUTE', 0, 'BLOCKING', 1, 20, NOW(), 0),
(940070001203, NULL, @bank_payment_rule_version_id, 'CURRENCY', 'MATCH_KEY', 'EQ', 'ABSOLUTE', 0, 'BLOCKING', 1, 30, NOW(), 0),
(940070001204, NULL, @bank_payment_rule_version_id, 'AMOUNT', 'AMOUNT', 'EQ', 'ABSOLUTE', 0, 'BLOCKING', 1, 40, NOW(), 0),
(940070001205, NULL, @bank_payment_rule_version_id, 'COUNTERPARTY_ACCOUNT', 'MATCH_KEY', 'EQ', 'ABSOLUTE', 0, 'BLOCKING', 0, 50, NOW(), 0);
