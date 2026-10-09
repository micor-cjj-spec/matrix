USE matrix_fi;

-- P0-IMP-05 compatibility rule.
-- Source-derived accounting semantics:
--   debit purchase/inventory net amount
--   debit input VAT
--   credit formal AP gross amount
-- Existing Matrix compatibility mappings keep 1405 / 2202 for purchase debit / AP.
--
-- IMPORTANT:
-- V6 specifies the account semantic "应交税费-应交增值税-进项税",
-- but the source document available to Matrix does not define the exact final numeric
-- sub-account code for the production chart of accounts.
-- Therefore INPUT_VAT is intentionally NOT guessed here.
-- Configure an ACTIVE matrix_fi_account_mapping row for faccount_key='INPUT_VAT'
-- before enabling the supplier-invoice accounting consumer.

INSERT INTO matrix_fi_accounting_rule
(fid, ftenant_id, forg_id, frule_code, frule_name, fevent_type,
 fpriority, fsource, fstatus, feffective_date, fexpiry_date,
 fcreate_by, fcreate_time, fmodify_by, fmodify_time, fdelete_flag, fversion)
VALUES
(95005001, NULL, NULL, 'PURCHASE_AP_COMPAT',
 '采购发票正式应付兼容核算规则', 'PURCHASE_AP_RECOGNITION',
 10, 'COMPATIBILITY', 'PUBLISHED', NULL, NULL,
 NULL, NOW(), NULL, NOW(), 0, 0)
ON DUPLICATE KEY UPDATE
 frule_name=VALUES(frule_name), fmodify_time=NOW();

INSERT INTO matrix_fi_accounting_rule_version
(fid, frule_id, fversion_no, fbook_id, fstatus, fsource,
 fdefinition_hash, fpublished_time, fcreate_by, fcreate_time, fdelete_flag)
VALUES
(95005101, 95005001, 1, 'DEFAULT', 'PUBLISHED', 'COMPATIBILITY',
 NULL, NOW(), NULL, NOW(), 0)
ON DUPLICATE KEY UPDATE fstatus=VALUES(fstatus);

INSERT INTO matrix_fi_accounting_rule_entry
(fid, frule_version_id, fline_no, fscope, fdirection,
 faccount_source_type, faccount_key, faccount_code,
 famount_expression, fskip_zero_amount, fsummary_template, fcurrency_expression,
 fstatus, fcreate_time, fdelete_flag)
VALUES
(95005201, 95005101, 1, 'ENTRY', 'DEBIT',
 'MAPPING', 'PURCHASE_INVOICE_DEBIT', NULL,
 'FIELD(entry.netAmount)', 0, '采购发票-${materialName}', 'FIELD(payload.currencyCode)',
 'ACTIVE', NOW(), 0),
(95005202, 95005101, 2, 'ENTRY', 'DEBIT',
 'MAPPING', 'INPUT_VAT', NULL,
 'FIELD(entry.taxAmount)', 1, '采购发票进项税-${materialName}', 'FIELD(payload.currencyCode)',
 'ACTIVE', NOW(), 0),
(95005203, 95005101, 3, 'HEADER', 'CREDIT',
 'MAPPING', 'FORMAL_AP', NULL,
 'FIELD(payload.grossAmount)', 0, '正式应付-${sourceDocumentNo}', 'FIELD(payload.currencyCode)',
 'ACTIVE', NOW(), 0)
ON DUPLICATE KEY UPDATE
 famount_expression=VALUES(famount_expression),
 fskip_zero_amount=VALUES(fskip_zero_amount),
 fsummary_template=VALUES(fsummary_template),
 fstatus='ACTIVE';

INSERT INTO matrix_fi_accounting_rule_dimension
(fid, frule_version_id, frule_entry_id, fdimension_code,
 fsource_path, frequired, fstatus, fcreate_time, fdelete_flag)
VALUES
(95005301, 95005101, 95005201, 'PROJECT', 'entry.projectId', 0, 'ACTIVE', NOW(), 0),
(95005302, 95005101, 95005201, 'COST_CENTER', 'entry.costCenterId', 0, 'ACTIVE', NOW(), 0),
(95005303, 95005101, 95005203, 'BUSINESS_PARTNER', 'payload.businessPartnerId', 1, 'ACTIVE', NOW(), 0)
ON DUPLICATE KEY UPDATE
 fsource_path=VALUES(fsource_path), frequired=VALUES(frequired), fstatus='ACTIVE';

INSERT INTO matrix_fi_account_mapping
(fid, ftenant_id, forg_id, fbook_id, faccount_key, faccount_code,
 fpriority, fsource, fstatus, feffective_date, fexpiry_date,
 fcreate_by, fcreate_time, fmodify_by, fmodify_time, fdelete_flag, fversion)
VALUES
(95005401, NULL, NULL, 'DEFAULT', 'PURCHASE_INVOICE_DEBIT', '1405',
 1, 'COMPATIBILITY', 'ACTIVE', NULL, NULL, NULL, NOW(), NULL, NOW(), 0, 0),
(95005402, NULL, NULL, 'DEFAULT', 'FORMAL_AP', '2202',
 1, 'COMPATIBILITY', 'ACTIVE', NULL, NULL, NULL, NOW(), NULL, NOW(), 0, 0)
ON DUPLICATE KEY UPDATE
 faccount_code=VALUES(faccount_code), fstatus='ACTIVE', fmodify_time=NOW();

-- Production example after finance confirms the exact account code:
-- INSERT INTO matrix_fi_account_mapping
-- (fid, ftenant_id, forg_id, fbook_id, faccount_key, faccount_code,
--  fpriority, fsource, fstatus, feffective_date, fexpiry_date,
--  fcreate_by, fcreate_time, fmodify_by, fmodify_time, fdelete_flag, fversion)
-- VALUES
-- (<id>, <tenant-or-null>, <org-or-null>, 'DEFAULT', 'INPUT_VAT',
--  '<FINANCE_CONFIRMED_INPUT_VAT_ACCOUNT_CODE>',
--  100, 'FORMAL', 'ACTIVE', NULL, NULL, NULL, NOW(), NULL, NOW(), 0, 0);
