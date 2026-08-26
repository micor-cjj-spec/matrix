-- P0-IMP-03 compatibility accounting seed.
-- IMPORTANT: this is NOT the formal accounting-policy source of truth.
-- The codes 1405 / 2202 are externalized only because the existing Matrix AR/AP implementation
-- already uses them for AP_ESTIMATE. Replace these mappings when the formal accounting-rule
-- standard table is available; Java flow code must not need to change.
-- Published rule-version rows are inserted with INSERT IGNORE so rerunning this migration does
-- not mutate an already published historical version.

SET @now = NOW();

INSERT INTO matrix_fi_accounting_rule
(fid, ftenant_id, forg_id, frule_code, frule_name, fevent_type, fpriority, fsource,
 fstatus, feffective_date, fexpiry_date, fcreate_time, fmodify_time, fdelete_flag, fversion)
VALUES
(930030001001, NULL, NULL, 'PURCHASE_INBOUND_ESTIMATE_COMPAT', '采购入库暂估-兼容规则',
 'PURCHASE_INBOUND_ESTIMATE_RECOGNITION', 10, 'COMPATIBILITY', 'PUBLISHED',
 NULL, NULL, @now, @now, 0, 0)
ON DUPLICATE KEY UPDATE
    frule_name = VALUES(frule_name),
    fevent_type = VALUES(fevent_type),
    fpriority = VALUES(fpriority),
    fsource = 'COMPATIBILITY',
    fstatus = 'PUBLISHED',
    fmodify_time = @now;

SELECT fid INTO @rule_id
FROM matrix_fi_accounting_rule
WHERE frule_code = 'PURCHASE_INBOUND_ESTIMATE_COMPAT'
LIMIT 1;

INSERT IGNORE INTO matrix_fi_accounting_rule_version
(fid, frule_id, fversion_no, fbook_id, fstatus, fsource, fdefinition_hash,
 fpublished_time, fcreate_time, fdelete_flag)
VALUES
(930030001101, @rule_id, 1, 'DEFAULT', 'PUBLISHED', 'COMPATIBILITY', NULL, @now, @now, 0);

SELECT fid INTO @version_id
FROM matrix_fi_accounting_rule_version
WHERE frule_id = @rule_id AND fversion_no = 1
LIMIT 1;

INSERT IGNORE INTO matrix_fi_accounting_rule_entry
(fid, frule_version_id, fline_no, fscope, fdirection,
 faccount_source_type, faccount_key, faccount_code,
 famount_expression, fsummary_template, fcurrency_expression,
 fstatus, fcreate_time, fdelete_flag)
VALUES
(930030001201, @version_id, 1, 'ENTRY', 'DEBIT', 'MAPPING',
 'PURCHASE_INBOUND_DEBIT', NULL, 'FIELD(entry.amount)',
 '采购入库暂估-${sourceDocumentNo}-${materialName}', 'FIELD(payload.currencyCode)',
 'ACTIVE', @now, 0),
(930030001202, @version_id, 2, 'HEADER', 'CREDIT', 'MAPPING',
 'ESTIMATED_AP', NULL, 'FIELD(payload.totalAmount)',
 '采购入库暂估应付-${businessPartnerName}', 'FIELD(payload.currencyCode)',
 'ACTIVE', @now, 0);

SELECT fid INTO @debit_entry_id
FROM matrix_fi_accounting_rule_entry
WHERE frule_version_id = @version_id AND fline_no = 1 LIMIT 1;
SELECT fid INTO @credit_entry_id
FROM matrix_fi_accounting_rule_entry
WHERE frule_version_id = @version_id AND fline_no = 2 LIMIT 1;

INSERT IGNORE INTO matrix_fi_accounting_rule_dimension
(fid, frule_version_id, frule_entry_id, fdimension_code, fsource_path,
 frequired, fstatus, fcreate_time, fdelete_flag)
VALUES
(930030001301, @version_id, @debit_entry_id, 'PROJECT', 'entry.projectId', 0, 'ACTIVE', @now, 0),
(930030001302, @version_id, @debit_entry_id, 'COST_CENTER', 'entry.costCenterId', 0, 'ACTIVE', @now, 0),
(930030001303, @version_id, @credit_entry_id, 'BUSINESS_PARTNER', 'payload.businessPartnerId', 1, 'ACTIVE', @now, 0);

INSERT INTO matrix_fi_account_mapping
(fid, fmapping_code, ftenant_id, forg_id, fbook_id,
 faccount_key, faccount_code, fpriority, fsource, fstatus,
 feffective_date, fexpiry_date, fcreate_time, fmodify_time, fdelete_flag, fversion)
VALUES
(930030001401, 'COMPAT_PURCHASE_INBOUND_DEBIT', NULL, NULL, 'DEFAULT',
 'PURCHASE_INBOUND_DEBIT', '1405', 10, 'COMPATIBILITY', 'ACTIVE',
 NULL, NULL, @now, @now, 0, 0),
(930030001402, 'COMPAT_ESTIMATED_AP', NULL, NULL, 'DEFAULT',
 'ESTIMATED_AP', '2202', 10, 'COMPATIBILITY', 'ACTIVE',
 NULL, NULL, @now, @now, 0, 0)
ON DUPLICATE KEY UPDATE
    faccount_key = VALUES(faccount_key),
    faccount_code = VALUES(faccount_code),
    fpriority = VALUES(fpriority),
    fsource = 'COMPATIBILITY',
    fstatus = 'ACTIVE',
    fmodify_time = @now;
