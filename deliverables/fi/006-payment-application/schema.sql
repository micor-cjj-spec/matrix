USE matrix_fi;

-- P0-IMP-06: Formal AP -> PaymentApplication reservation -> approval business event.
-- Run after deliverables/fi/005-formal-ap/schema.sql.

ALTER TABLE matrix_fi_ap_payable
    ADD COLUMN freserved_amount DECIMAL(20,2) NOT NULL DEFAULT 0
        COMMENT '已被付款申请占用、尚未完成核销的金额'
        AFTER fsettled_amount,
    ADD COLUMN fdue_date DATE NULL AFTER fdate,
    ADD COLUMN fpayment_term_code VARCHAR(100) NULL AFTER fdue_date;

CREATE INDEX idx_matrix_fi_ap_payable_due_status
    ON matrix_fi_ap_payable (ftenant_id, forg_id, fstatus, fdue_date);

CREATE TABLE IF NOT EXISTS matrix_fi_payment_application (
    fid BIGINT NOT NULL,
    ftenant_id VARCHAR(64) NOT NULL,
    forg_id BIGINT NOT NULL,
    fnumber VARCHAR(100) NOT NULL,
    fdate DATE NOT NULL,
    frequester_id BIGINT NULL,
    fbusiness_partner_id VARCHAR(100) NOT NULL,
    fbusiness_partner_code VARCHAR(100) NULL,
    fbusiness_partner_name VARCHAR(255) NULL,
    fcurrency_code VARCHAR(20) NOT NULL,
    famount DECIMAL(20,2) NOT NULL,
    ffund_plan_id VARCHAR(100) NULL,
    fplanned_pay_date DATE NULL,
    fpayment_method VARCHAR(32) NULL,
    fpayee_bank_account_id VARCHAR(100) NULL,
    fpayee_account_name VARCHAR(255) NULL,
    fpayee_bank_name VARCHAR(255) NULL,
    fpayee_bank_account_no VARCHAR(100) NULL,
    fevidence_check_status VARCHAR(32) NOT NULL DEFAULT 'PENDING'
        COMMENT 'PENDING/PASSED/FAILED',
    fbudget_check_status VARCHAR(32) NOT NULL DEFAULT 'PENDING'
        COMMENT 'PENDING/PASSED/FAILED/NOT_REQUIRED',
    fbudget_check_id VARCHAR(100) NULL,
    fbudget_available_amount DECIMAL(20,2) NULL,
    fbudget_check_message VARCHAR(500) NULL,
    fbudget_snapshot_json JSON NULL,
    fstatus VARCHAR(32) NOT NULL
        COMMENT 'DRAFT/SUBMITTED/APPROVED/REJECTED/CANCELLED',
    fapproval_status VARCHAR(32) NOT NULL
        COMMENT 'DRAFT/SUBMITTED/AUDITED/REJECTED',
    fexecution_status VARCHAR(32) NOT NULL DEFAULT 'NOT_EXECUTED'
        COMMENT 'NOT_EXECUTED/PARTIAL/COMPLETE',
    fbotp_idempotency_key VARCHAR(160) NULL,
    fsource_system_code VARCHAR(64) NULL,
    fsource_document_type VARCHAR(100) NULL,
    fsource_document_id VARCHAR(100) NULL,
    fsource_execution_id VARCHAR(100) NULL,
    fremark VARCHAR(1000) NULL,
    freject_reason VARCHAR(1000) NULL,
    fapproved_by BIGINT NULL,
    fapproved_time DATETIME NULL,
    fcreate_by BIGINT NULL,
    fcreate_time DATETIME NOT NULL,
    fmodify_by BIGINT NULL,
    fmodify_time DATETIME NOT NULL,
    fdelete_flag TINYINT NOT NULL DEFAULT 0,
    fversion INT NOT NULL DEFAULT 0,
    PRIMARY KEY (fid),
    UNIQUE KEY uk_matrix_fi_payment_application_number (ftenant_id, fnumber),
    UNIQUE KEY uk_matrix_fi_payment_application_botp (
        ftenant_id, fbotp_idempotency_key, fdelete_flag
    ),
    KEY idx_matrix_fi_payment_application_partner (
        ftenant_id, fbusiness_partner_id, fstatus
    ),
    KEY idx_matrix_fi_payment_application_status (
        ftenant_id, forg_id, fstatus, fplanned_pay_date
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='付款申请';

CREATE TABLE IF NOT EXISTS matrix_fi_payment_application_allocation (
    fid BIGINT NOT NULL,
    ftenant_id VARCHAR(64) NOT NULL,
    forg_id BIGINT NOT NULL,
    fpayment_application_id BIGINT NOT NULL,
    fpayable_id BIGINT NOT NULL,
    fapplied_amount DECIMAL(20,2) NOT NULL,
    freserved_amount DECIMAL(20,2) NOT NULL,
    fstatus VARCHAR(32) NOT NULL
        COMMENT 'RESERVED/RELEASED/CONSUMED',
    fcreate_by BIGINT NULL,
    fcreate_time DATETIME NOT NULL,
    fmodify_by BIGINT NULL,
    fmodify_time DATETIME NOT NULL,
    fdelete_flag TINYINT NOT NULL DEFAULT 0,
    fversion INT NOT NULL DEFAULT 0,
    PRIMARY KEY (fid),
    UNIQUE KEY uk_matrix_fi_payment_application_alloc (
        fpayment_application_id, fpayable_id, fdelete_flag
    ),
    KEY idx_matrix_fi_payment_application_alloc_payable (
        ftenant_id, fpayable_id, fstatus
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='付款申请对应应付占用';

CREATE TABLE IF NOT EXISTS matrix_fi_payment_application_evidence (
    fid BIGINT NOT NULL,
    ftenant_id VARCHAR(64) NOT NULL,
    forg_id BIGINT NOT NULL,
    fpayment_application_id BIGINT NOT NULL,
    fevidence_type VARCHAR(64) NOT NULL
        COMMENT 'CONTRACT/INVOICE/ACCEPTANCE/INBOUND/RECONCILIATION/OTHER',
    fsource_system_code VARCHAR(64) NULL,
    fsource_document_type VARCHAR(100) NULL,
    fsource_document_id VARCHAR(100) NULL,
    fsource_document_no VARCHAR(100) NULL,
    frequired TINYINT NOT NULL DEFAULT 1,
    fverification_status VARCHAR(32) NOT NULL DEFAULT 'PENDING'
        COMMENT 'PENDING/VERIFIED/REJECTED',
    fremark VARCHAR(500) NULL,
    fcreate_by BIGINT NULL,
    fcreate_time DATETIME NOT NULL,
    fmodify_by BIGINT NULL,
    fmodify_time DATETIME NOT NULL,
    fdelete_flag TINYINT NOT NULL DEFAULT 0,
    fversion INT NOT NULL DEFAULT 0,
    PRIMARY KEY (fid),
    KEY idx_matrix_fi_payment_application_evidence_app (
        ftenant_id, fpayment_application_id, frequired, fverification_status
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='付款申请依据材料快照';

CREATE TABLE IF NOT EXISTS matrix_fi_business_event_outbox (
    fid BIGINT NOT NULL,
    ftenant_id VARCHAR(64) NOT NULL,
    forg_id BIGINT NULL,
    fevent_id VARCHAR(100) NOT NULL,
    fevent_type VARCHAR(100) NOT NULL,
    fevent_version INT NOT NULL DEFAULT 1,
    fproducer_service VARCHAR(100) NOT NULL,
    fdomain_code VARCHAR(64) NOT NULL,
    faggregate_type VARCHAR(100) NOT NULL,
    faggregate_id VARCHAR(100) NOT NULL,
    faggregate_version BIGINT NOT NULL DEFAULT 0,
    fsource_system_code VARCHAR(64) NOT NULL,
    fsource_document_type VARCHAR(100) NOT NULL,
    fsource_document_id VARCHAR(100) NOT NULL,
    fsource_document_no VARCHAR(100) NULL,
    fbusiness_date DATE NULL,
    fcorrelation_id VARCHAR(100) NULL,
    fcausation_id VARCHAR(100) NULL,
    ftrace_id VARCHAR(100) NULL,
    foperator_id BIGINT NULL,
    frouting_key VARCHAR(200) NOT NULL,
    fpayload_json JSON NOT NULL,
    fstatus VARCHAR(32) NOT NULL COMMENT 'PENDING/PUBLISHING/PUBLISHED/FAILED/DEAD',
    fretry_count INT NOT NULL DEFAULT 0,
    fmax_retry INT NOT NULL DEFAULT 10,
    fnext_retry_time DATETIME NULL,
    fclaim_token VARCHAR(100) NULL,
    fclaim_time DATETIME NULL,
    fsent_time DATETIME NULL,
    flast_error VARCHAR(1000) NULL,
    fcreate_by BIGINT NULL,
    fcreate_time DATETIME NOT NULL,
    fmodify_by BIGINT NULL,
    fmodify_time DATETIME NOT NULL,
    fdelete_flag TINYINT NOT NULL DEFAULT 0,
    fversion INT NOT NULL DEFAULT 0,
    PRIMARY KEY (fid),
    UNIQUE KEY uk_matrix_fi_business_event_outbox_event (ftenant_id, fevent_id),
    KEY idx_matrix_fi_business_event_outbox_due (fstatus, fnext_retry_time, fid),
    KEY idx_matrix_fi_business_event_outbox_source (
        ftenant_id, fsource_document_type, fsource_document_id
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='FI业务事件Transactional Outbox';
