-- P0-IMP-04 Reconciliation Framework first implementation: P2P 3-Way Match
-- Database: matrix_fi

CREATE TABLE IF NOT EXISTS matrix_fi_reconciliation_rule (
    fid BIGINT NOT NULL,
    ftenant_id VARCHAR(64) NULL,
    fcode VARCHAR(128) NOT NULL,
    fname VARCHAR(255) NOT NULL,
    fscenario_type VARCHAR(64) NOT NULL,
    fstatus VARCHAR(32) NOT NULL,
    fcurrent_version INT NOT NULL DEFAULT 1,
    fpriority INT NOT NULL DEFAULT 0,
    feffective_date DATE NULL,
    fexpiry_date DATE NULL,
    fcreate_by BIGINT NULL,
    fcreate_time DATETIME NOT NULL,
    fmodify_by BIGINT NULL,
    fmodify_time DATETIME NOT NULL,
    fdelete_flag TINYINT NOT NULL DEFAULT 0,
    fversion INT NOT NULL DEFAULT 0,
    PRIMARY KEY (fid),
    UNIQUE KEY uk_fi_reconciliation_rule (ftenant_id, fcode, fdelete_flag),
    KEY idx_fi_reconciliation_rule_scenario (fscenario_type, fstatus)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对账规则';

CREATE TABLE IF NOT EXISTS matrix_fi_reconciliation_rule_version (
    fid BIGINT NOT NULL,
    ftenant_id VARCHAR(64) NULL,
    frule_id BIGINT NOT NULL,
    fversion_no INT NOT NULL,
    fstatus VARCHAR(32) NOT NULL,
    fdefinition_json JSON NULL,
    fdefinition_hash VARCHAR(64) NULL,
    fpublished_by BIGINT NULL,
    fpublished_time DATETIME NULL,
    fcreate_time DATETIME NOT NULL,
    fdelete_flag TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (fid),
    UNIQUE KEY uk_fi_reconciliation_rule_version (frule_id, fversion_no, fdelete_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对账规则不可变版本';

CREATE TABLE IF NOT EXISTS matrix_fi_reconciliation_rule_field (
    fid BIGINT NOT NULL,
    ftenant_id VARCHAR(64) NULL,
    frule_version_id BIGINT NOT NULL,
    ffield_code VARCHAR(64) NOT NULL,
    ffield_role VARCHAR(32) NOT NULL,
    fcompare_operator VARCHAR(32) NOT NULL,
    ftolerance_type VARCHAR(32) NOT NULL DEFAULT 'ABSOLUTE',
    ftolerance_value DECIMAL(20,6) NOT NULL DEFAULT 0,
    fseverity VARCHAR(32) NOT NULL DEFAULT 'BLOCKING',
    frequired TINYINT NOT NULL DEFAULT 1,
    fsort_no INT NOT NULL DEFAULT 0,
    fcreate_time DATETIME NOT NULL,
    fdelete_flag TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (fid),
    UNIQUE KEY uk_fi_reconciliation_rule_field (frule_version_id, ffield_code, fdelete_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对账规则字段';

CREATE TABLE IF NOT EXISTS matrix_fi_reconciliation_batch (
    fid BIGINT NOT NULL,
    ftenant_id VARCHAR(64) NOT NULL,
    forg_id BIGINT NOT NULL,
    fbatch_no VARCHAR(64) NOT NULL,
    fscenario_type VARCHAR(64) NOT NULL,
    frule_code VARCHAR(128) NOT NULL,
    frule_version INT NOT NULL,
    frequest_id VARCHAR(128) NOT NULL,
    fsource_system_code VARCHAR(64) NOT NULL,
    fsource_document_type VARCHAR(64) NOT NULL,
    fsource_document_id VARCHAR(128) NOT NULL,
    fsource_document_no VARCHAR(128) NULL,
    fstatus VARCHAR(32) NOT NULL,
    fresult VARCHAR(32) NULL,
    ftotal_case_count INT NOT NULL DEFAULT 0,
    fmatched_count INT NOT NULL DEFAULT 0,
    fpartial_count INT NOT NULL DEFAULT 0,
    fdifference_count INT NOT NULL DEFAULT 0,
    funmatched_count INT NOT NULL DEFAULT 0,
    fstart_time DATETIME NOT NULL,
    ffinish_time DATETIME NULL,
    fcreate_time DATETIME NOT NULL,
    fmodify_time DATETIME NULL,
    fdelete_flag TINYINT NOT NULL DEFAULT 0,
    fversion INT NOT NULL DEFAULT 0,
    PRIMARY KEY (fid),
    UNIQUE KEY uk_fi_reconciliation_batch_request (ftenant_id, frequest_id, fdelete_flag),
    UNIQUE KEY uk_fi_reconciliation_batch_no (ftenant_id, fbatch_no, fdelete_flag),
    KEY idx_fi_reconciliation_batch_source (ftenant_id, fsource_document_type, fsource_document_id),
    KEY idx_fi_reconciliation_batch_status (ftenant_id, fscenario_type, fstatus, fstart_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对账执行批次';

CREATE TABLE IF NOT EXISTS matrix_fi_reconciliation_case (
    fid BIGINT NOT NULL,
    ftenant_id VARCHAR(64) NOT NULL,
    forg_id BIGINT NOT NULL,
    fbatch_id BIGINT NOT NULL,
    fcase_no VARCHAR(128) NOT NULL,
    fcase_key VARCHAR(128) NOT NULL,
    fresult VARCHAR(32) NOT NULL,
    favailable_quantity DECIMAL(20,6) NULL,
    fsnapshot_json JSON NULL,
    fstatus VARCHAR(32) NOT NULL,
    fcreate_time DATETIME NOT NULL,
    fmodify_time DATETIME NOT NULL,
    fdelete_flag TINYINT NOT NULL DEFAULT 0,
    fversion INT NOT NULL DEFAULT 0,
    PRIMARY KEY (fid),
    UNIQUE KEY uk_fi_reconciliation_case (fbatch_id, fcase_key, fdelete_flag),
    KEY idx_fi_reconciliation_case_result (ftenant_id, fresult, fcreate_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对账Case';

CREATE TABLE IF NOT EXISTS matrix_fi_reconciliation_participant (
    fid BIGINT NOT NULL,
    ftenant_id VARCHAR(64) NOT NULL,
    forg_id BIGINT NOT NULL,
    fcase_id BIGINT NOT NULL,
    fparticipant_role VARCHAR(64) NOT NULL,
    fsystem_code VARCHAR(64) NOT NULL,
    fdocument_type VARCHAR(64) NOT NULL,
    fdocument_id VARCHAR(128) NOT NULL,
    fdocument_no VARCHAR(128) NULL,
    fentry_id VARCHAR(128) NULL,
    fbusiness_partner_id BIGINT NULL,
    fcurrency_code VARCHAR(32) NULL,
    fbusiness_date DATE NULL,
    fsnapshot_json JSON NOT NULL,
    fcreate_time DATETIME NOT NULL,
    fdelete_flag TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (fid),
    KEY idx_fi_reconciliation_participant_case (ftenant_id, fcase_id, fparticipant_role),
    KEY idx_fi_reconciliation_participant_document (ftenant_id, fdocument_type, fdocument_id, fentry_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对账参与方快照';

CREATE TABLE IF NOT EXISTS matrix_fi_reconciliation_match (
    fid BIGINT NOT NULL,
    ftenant_id VARCHAR(64) NOT NULL,
    forg_id BIGINT NOT NULL,
    fcase_id BIGINT NOT NULL,
    fmatch_type VARCHAR(64) NOT NULL,
    fmatched_quantity DECIMAL(20,6) NOT NULL DEFAULT 0,
    fexpected_value_json JSON NULL,
    factual_value_json JSON NULL,
    fstatus VARCHAR(32) NOT NULL,
    fcreate_time DATETIME NOT NULL,
    fdelete_flag TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (fid),
    KEY idx_fi_reconciliation_match_case (ftenant_id, fcase_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对账匹配快照';

CREATE TABLE IF NOT EXISTS matrix_fi_reconciliation_difference (
    fid BIGINT NOT NULL,
    ftenant_id VARCHAR(64) NOT NULL,
    forg_id BIGINT NOT NULL,
    fcase_id BIGINT NOT NULL,
    fdifference_code VARCHAR(64) NOT NULL,
    ffield_code VARCHAR(128) NOT NULL,
    fexpected_value VARCHAR(1000) NULL,
    factual_value VARCHAR(1000) NULL,
    fseverity VARCHAR(32) NOT NULL,
    fmessage VARCHAR(1000) NULL,
    fstatus VARCHAR(32) NOT NULL,
    fcreate_time DATETIME NOT NULL,
    fmodify_time DATETIME NOT NULL,
    fdelete_flag TINYINT NOT NULL DEFAULT 0,
    fversion INT NOT NULL DEFAULT 0,
    PRIMARY KEY (fid),
    KEY idx_fi_reconciliation_difference_case (ftenant_id, fcase_id, fstatus),
    KEY idx_fi_reconciliation_difference_code (ftenant_id, fdifference_code, fcreate_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对账差异';

CREATE TABLE IF NOT EXISTS matrix_fi_reconciliation_resolution (
    fid BIGINT NOT NULL,
    ftenant_id VARCHAR(64) NOT NULL,
    forg_id BIGINT NOT NULL,
    fcase_id BIGINT NOT NULL,
    fdifference_id BIGINT NULL,
    fdecision VARCHAR(64) NOT NULL,
    freason VARCHAR(1000) NOT NULL,
    fapproval_status VARCHAR(32) NOT NULL,
    fapproved_by BIGINT NULL,
    fapproved_time DATETIME NULL,
    fcreate_by BIGINT NULL,
    fcreate_time DATETIME NOT NULL,
    fmodify_by BIGINT NULL,
    fmodify_time DATETIME NOT NULL,
    fdelete_flag TINYINT NOT NULL DEFAULT 0,
    fversion INT NOT NULL DEFAULT 0,
    PRIMARY KEY (fid),
    KEY idx_fi_reconciliation_resolution_case (ftenant_id, fcase_id, fapproval_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对账差异处理';

-- P0 rule. Published versions are immutable; this seed only creates V1 if absent.
INSERT INTO matrix_fi_reconciliation_rule
(fid, ftenant_id, fcode, fname, fscenario_type, fstatus, fcurrent_version, fpriority,
 fcreate_time, fmodify_time, fdelete_flag, fversion)
SELECT 940040001001, NULL, 'P2P_3WAY_MATCH', '采购订单-采购入库-供应商发票三单匹配',
       'P2P_3WAY_MATCH', 'PUBLISHED', 1, 100, NOW(), NOW(), 0, 0
WHERE NOT EXISTS (
    SELECT 1 FROM matrix_fi_reconciliation_rule
     WHERE ftenant_id IS NULL AND fcode = 'P2P_3WAY_MATCH' AND fdelete_flag = 0
);

SELECT fid INTO @p2p_rule_id
FROM matrix_fi_reconciliation_rule
WHERE ftenant_id IS NULL AND fcode = 'P2P_3WAY_MATCH' AND fdelete_flag = 0
ORDER BY fid LIMIT 1;

INSERT IGNORE INTO matrix_fi_reconciliation_rule_version
(fid, ftenant_id, frule_id, fversion_no, fstatus, fdefinition_json, fdefinition_hash,
 fpublished_time, fcreate_time, fdelete_flag)
VALUES
(940040001101, NULL, @p2p_rule_id, 1, 'PUBLISHED',
 JSON_OBJECT('tolerance','ZERO','partialInvoiceAllowed',true,'blockingDifferences',true),
 NULL, NOW(), NOW(), 0);

SELECT fid INTO @p2p_rule_version_id
FROM matrix_fi_reconciliation_rule_version
WHERE frule_id = @p2p_rule_id AND fversion_no = 1 AND fdelete_flag = 0
ORDER BY fid LIMIT 1;

INSERT IGNORE INTO matrix_fi_reconciliation_rule_field
(fid, ftenant_id, frule_version_id, ffield_code, ffield_role, fcompare_operator,
 ftolerance_type, ftolerance_value, fseverity, frequired, fsort_no, fcreate_time, fdelete_flag)
VALUES
(940040001201, NULL, @p2p_rule_version_id, 'BUSINESS_PARTNER', 'MATCH_KEY', 'EQ', 'ABSOLUTE', 0, 'BLOCKING', 1, 10, NOW(), 0),
(940040001202, NULL, @p2p_rule_version_id, 'CURRENCY', 'MATCH_KEY', 'EQ', 'ABSOLUTE', 0, 'BLOCKING', 1, 20, NOW(), 0),
(940040001203, NULL, @p2p_rule_version_id, 'MATERIAL', 'MATCH_KEY', 'EQ', 'ABSOLUTE', 0, 'BLOCKING', 1, 30, NOW(), 0),
(940040001204, NULL, @p2p_rule_version_id, 'SPECIFICATION', 'MATCH_KEY', 'EQ', 'ABSOLUTE', 0, 'BLOCKING', 0, 40, NOW(), 0),
(940040001205, NULL, @p2p_rule_version_id, 'QUANTITY', 'QUANTITY', 'LE_AVAILABLE', 'ABSOLUTE', 0, 'BLOCKING', 1, 50, NOW(), 0),
(940040001206, NULL, @p2p_rule_version_id, 'UNIT_PRICE', 'PRICE', 'EQ', 'ABSOLUTE', 0, 'BLOCKING', 1, 60, NOW(), 0),
(940040001207, NULL, @p2p_rule_version_id, 'NET_AMOUNT', 'AMOUNT', 'EQ', 'ABSOLUTE', 0, 'BLOCKING', 1, 70, NOW(), 0),
(940040001208, NULL, @p2p_rule_version_id, 'TAX_RATE', 'TAX', 'EQ', 'ABSOLUTE', 0, 'BLOCKING', 1, 80, NOW(), 0),
(940040001209, NULL, @p2p_rule_version_id, 'TAX_AMOUNT', 'TAX', 'EQ', 'ABSOLUTE', 0, 'BLOCKING', 1, 90, NOW(), 0),
(940040001210, NULL, @p2p_rule_version_id, 'GROSS_AMOUNT', 'AMOUNT', 'EQ', 'ABSOLUTE', 0, 'BLOCKING', 1, 100, NOW(), 0);
