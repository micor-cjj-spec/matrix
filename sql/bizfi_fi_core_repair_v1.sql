CREATE TABLE IF NOT EXISTS bizfi_fi_voucher (
  fid BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT 'Tenant id',
  org_id VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT 'Organization id',
  book_id VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT 'Book id',
  source_request_id VARCHAR(128) NULL COMMENT 'OpenAPI source request id',
  fnumber VARCHAR(64) NOT NULL COMMENT 'Voucher number',
  fdate DATE NOT NULL COMMENT 'Voucher date',
  fsummary VARCHAR(500) NULL COMMENT 'Summary',
  famount DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT 'Amount',
  fstatus VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'Status: DRAFT/SUBMITTED/AUDITED/POSTED/REVERSED',
  fcreated_by VARCHAR(64) NULL COMMENT 'Created by',
  fcreated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  faudited_by VARCHAR(64) NULL COMMENT 'Audited by',
  faudited_time DATETIME NULL COMMENT 'Audited time',
  fposted_by VARCHAR(64) NULL COMMENT 'Posted by',
  fposted_time DATETIME NULL COMMENT 'Posted time',
  fremark VARCHAR(500) NULL COMMENT 'Remark',
  KEY idx_voucher_status (fstatus),
  KEY idx_voucher_date (fdate),
  KEY idx_voucher_number (fnumber),
  KEY idx_voucher_openapi_scope (tenant_id, org_id, book_id, fstatus, fdate),
  KEY idx_voucher_source_request (source_request_id),
  UNIQUE KEY uk_voucher_openapi_source (tenant_id, source_request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Finance voucher';

CREATE TABLE IF NOT EXISTS bizfi_fi_arap_doc (
  fid BIGINT PRIMARY KEY AUTO_INCREMENT,
  fdoctype VARCHAR(40) NOT NULL COMMENT 'AP/AP_ESTIMATE/AP_PAYMENT_APPLY/AP_PAYMENT_PROCESS/AR/AR_ESTIMATE/AR_SETTLEMENT',
  fnumber VARCHAR(64) NOT NULL,
  fdate DATE NOT NULL,
  fcounterparty VARCHAR(128) NOT NULL,
  famount DECIMAL(18,2) NOT NULL,
  fstatus VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  fremark VARCHAR(500) NULL,
  fpay_method VARCHAR(32) NULL,
  fplanned_pay_date DATE NULL,
  fsettle_method VARCHAR(32) NULL,
  fwriteoff_detail VARCHAR(500) NULL,
  fsource_bill_no VARCHAR(64) NULL,
  fapplied_amount DECIMAL(23,10) NOT NULL DEFAULT 0,
  freserved_amount DECIMAL(23,10) NOT NULL DEFAULT 0,
  fremaining_amount DECIMAL(23,10) NULL,
  fpush_status VARCHAR(32) NOT NULL DEFAULT 'NOT_PUSHED',
  fbotp_idempotency_key VARCHAR(255) NULL,
  fsource_system VARCHAR(64) NULL,
  fsource_document_type VARCHAR(64) NULL,
  fsource_document_id VARCHAR(128) NULL,
  fsource_execution_id VARCHAR(64) NULL,
  fversion INT NOT NULL DEFAULT 0,
  fvoucher_id BIGINT NULL,
  fvoucher_number VARCHAR(64) NULL,
  faudited_by VARCHAR(64) NULL,
  faudited_time DATETIME NULL,
  KEY idx_arap_type (fdoctype),
  KEY idx_arap_status (fstatus),
  KEY idx_arap_date (fdate),
  KEY idx_arap_voucher (fvoucher_id),
  UNIQUE KEY uk_arap_number (fdoctype, fnumber),
  UNIQUE KEY uk_bizfi_fi_arap_doc_botp_key (fbotp_idempotency_key),
  KEY idx_bizfi_fi_arap_doc_botp_source (fsource_system, fsource_document_type, fsource_document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Finance AR/AP document';

DELIMITER $$

DROP PROCEDURE IF EXISTS bizfi_fi_core_repair$$
CREATE PROCEDURE bizfi_fi_core_repair()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'bizfi_fi_voucher' AND column_name = 'tenant_id') THEN
    ALTER TABLE bizfi_fi_voucher ADD COLUMN tenant_id VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT 'Tenant id' AFTER fid;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'bizfi_fi_voucher' AND column_name = 'org_id') THEN
    ALTER TABLE bizfi_fi_voucher ADD COLUMN org_id VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT 'Organization id' AFTER tenant_id;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'bizfi_fi_voucher' AND column_name = 'book_id') THEN
    ALTER TABLE bizfi_fi_voucher ADD COLUMN book_id VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT 'Book id' AFTER org_id;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'bizfi_fi_voucher' AND column_name = 'source_request_id') THEN
    ALTER TABLE bizfi_fi_voucher ADD COLUMN source_request_id VARCHAR(128) NULL COMMENT 'OpenAPI source request id' AFTER book_id;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'bizfi_fi_voucher' AND column_name = 'fstatus') THEN
    ALTER TABLE bizfi_fi_voucher ADD COLUMN fstatus VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'Status' AFTER famount;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'bizfi_fi_voucher' AND column_name = 'fcreated_by') THEN
    ALTER TABLE bizfi_fi_voucher ADD COLUMN fcreated_by VARCHAR(64) NULL COMMENT 'Created by' AFTER fstatus;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'bizfi_fi_voucher' AND column_name = 'fcreated_time') THEN
    ALTER TABLE bizfi_fi_voucher ADD COLUMN fcreated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time' AFTER fcreated_by;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'bizfi_fi_voucher' AND column_name = 'faudited_by') THEN
    ALTER TABLE bizfi_fi_voucher ADD COLUMN faudited_by VARCHAR(64) NULL COMMENT 'Audited by' AFTER fcreated_time;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'bizfi_fi_voucher' AND column_name = 'faudited_time') THEN
    ALTER TABLE bizfi_fi_voucher ADD COLUMN faudited_time DATETIME NULL COMMENT 'Audited time' AFTER faudited_by;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'bizfi_fi_voucher' AND column_name = 'fposted_by') THEN
    ALTER TABLE bizfi_fi_voucher ADD COLUMN fposted_by VARCHAR(64) NULL COMMENT 'Posted by' AFTER faudited_time;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'bizfi_fi_voucher' AND column_name = 'fposted_time') THEN
    ALTER TABLE bizfi_fi_voucher ADD COLUMN fposted_time DATETIME NULL COMMENT 'Posted time' AFTER fposted_by;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'bizfi_fi_voucher' AND column_name = 'fremark') THEN
    ALTER TABLE bizfi_fi_voucher ADD COLUMN fremark VARCHAR(500) NULL COMMENT 'Remark' AFTER fposted_time;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'bizfi_fi_arap_doc' AND column_name = 'fpay_method') THEN
    ALTER TABLE bizfi_fi_arap_doc ADD COLUMN fpay_method VARCHAR(32) NULL AFTER fremark;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'bizfi_fi_arap_doc' AND column_name = 'fplanned_pay_date') THEN
    ALTER TABLE bizfi_fi_arap_doc ADD COLUMN fplanned_pay_date DATE NULL AFTER fpay_method;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'bizfi_fi_arap_doc' AND column_name = 'fsettle_method') THEN
    ALTER TABLE bizfi_fi_arap_doc ADD COLUMN fsettle_method VARCHAR(32) NULL AFTER fplanned_pay_date;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'bizfi_fi_arap_doc' AND column_name = 'fwriteoff_detail') THEN
    ALTER TABLE bizfi_fi_arap_doc ADD COLUMN fwriteoff_detail VARCHAR(500) NULL AFTER fsettle_method;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'bizfi_fi_arap_doc' AND column_name = 'fsource_bill_no') THEN
    ALTER TABLE bizfi_fi_arap_doc ADD COLUMN fsource_bill_no VARCHAR(64) NULL AFTER fwriteoff_detail;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'bizfi_fi_arap_doc' AND column_name = 'fapplied_amount') THEN
    ALTER TABLE bizfi_fi_arap_doc ADD COLUMN fapplied_amount DECIMAL(23,10) NOT NULL DEFAULT 0 AFTER fsource_bill_no;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'bizfi_fi_arap_doc' AND column_name = 'freserved_amount') THEN
    ALTER TABLE bizfi_fi_arap_doc ADD COLUMN freserved_amount DECIMAL(23,10) NOT NULL DEFAULT 0 AFTER fapplied_amount;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'bizfi_fi_arap_doc' AND column_name = 'fremaining_amount') THEN
    ALTER TABLE bizfi_fi_arap_doc ADD COLUMN fremaining_amount DECIMAL(23,10) NULL AFTER freserved_amount;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'bizfi_fi_arap_doc' AND column_name = 'fpush_status') THEN
    ALTER TABLE bizfi_fi_arap_doc ADD COLUMN fpush_status VARCHAR(32) NOT NULL DEFAULT 'NOT_PUSHED' AFTER fremaining_amount;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'bizfi_fi_arap_doc' AND column_name = 'fbotp_idempotency_key') THEN
    ALTER TABLE bizfi_fi_arap_doc ADD COLUMN fbotp_idempotency_key VARCHAR(255) NULL AFTER fpush_status;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'bizfi_fi_arap_doc' AND column_name = 'fsource_system') THEN
    ALTER TABLE bizfi_fi_arap_doc ADD COLUMN fsource_system VARCHAR(64) NULL AFTER fbotp_idempotency_key;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'bizfi_fi_arap_doc' AND column_name = 'fsource_document_type') THEN
    ALTER TABLE bizfi_fi_arap_doc ADD COLUMN fsource_document_type VARCHAR(64) NULL AFTER fsource_system;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'bizfi_fi_arap_doc' AND column_name = 'fsource_document_id') THEN
    ALTER TABLE bizfi_fi_arap_doc ADD COLUMN fsource_document_id VARCHAR(128) NULL AFTER fsource_document_type;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'bizfi_fi_arap_doc' AND column_name = 'fsource_execution_id') THEN
    ALTER TABLE bizfi_fi_arap_doc ADD COLUMN fsource_execution_id VARCHAR(64) NULL AFTER fsource_document_id;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'bizfi_fi_arap_doc' AND column_name = 'fversion') THEN
    ALTER TABLE bizfi_fi_arap_doc ADD COLUMN fversion INT NOT NULL DEFAULT 0 AFTER fsource_execution_id;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'bizfi_fi_arap_doc' AND column_name = 'fvoucher_id') THEN
    ALTER TABLE bizfi_fi_arap_doc ADD COLUMN fvoucher_id BIGINT NULL AFTER fversion;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'bizfi_fi_arap_doc' AND column_name = 'fvoucher_number') THEN
    ALTER TABLE bizfi_fi_arap_doc ADD COLUMN fvoucher_number VARCHAR(64) NULL AFTER fvoucher_id;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'bizfi_fi_arap_doc' AND column_name = 'faudited_by') THEN
    ALTER TABLE bizfi_fi_arap_doc ADD COLUMN faudited_by VARCHAR(64) NULL AFTER fvoucher_number;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'bizfi_fi_arap_doc' AND column_name = 'faudited_time') THEN
    ALTER TABLE bizfi_fi_arap_doc ADD COLUMN faudited_time DATETIME NULL AFTER faudited_by;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'bizfi_fi_voucher' AND index_name = 'idx_voucher_status') THEN
    ALTER TABLE bizfi_fi_voucher ADD INDEX idx_voucher_status (fstatus);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'bizfi_fi_voucher' AND index_name = 'idx_voucher_date') THEN
    ALTER TABLE bizfi_fi_voucher ADD INDEX idx_voucher_date (fdate);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'bizfi_fi_voucher' AND index_name = 'idx_voucher_number') THEN
    ALTER TABLE bizfi_fi_voucher ADD INDEX idx_voucher_number (fnumber);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'bizfi_fi_voucher' AND index_name = 'idx_voucher_openapi_scope') THEN
    ALTER TABLE bizfi_fi_voucher ADD INDEX idx_voucher_openapi_scope (tenant_id, org_id, book_id, fstatus, fdate);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'bizfi_fi_voucher' AND index_name = 'idx_voucher_source_request') THEN
    ALTER TABLE bizfi_fi_voucher ADD INDEX idx_voucher_source_request (source_request_id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'bizfi_fi_voucher' AND index_name = 'uk_voucher_openapi_source') THEN
    ALTER TABLE bizfi_fi_voucher ADD UNIQUE KEY uk_voucher_openapi_source (tenant_id, source_request_id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'bizfi_fi_arap_doc' AND index_name = 'idx_arap_type') THEN
    ALTER TABLE bizfi_fi_arap_doc ADD INDEX idx_arap_type (fdoctype);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'bizfi_fi_arap_doc' AND index_name = 'idx_arap_status') THEN
    ALTER TABLE bizfi_fi_arap_doc ADD INDEX idx_arap_status (fstatus);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'bizfi_fi_arap_doc' AND index_name = 'idx_arap_date') THEN
    ALTER TABLE bizfi_fi_arap_doc ADD INDEX idx_arap_date (fdate);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'bizfi_fi_arap_doc' AND index_name = 'idx_arap_voucher') THEN
    ALTER TABLE bizfi_fi_arap_doc ADD INDEX idx_arap_voucher (fvoucher_id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'bizfi_fi_arap_doc' AND index_name = 'uk_arap_number') THEN
    ALTER TABLE bizfi_fi_arap_doc ADD UNIQUE KEY uk_arap_number (fdoctype, fnumber);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'bizfi_fi_arap_doc' AND index_name = 'uk_bizfi_fi_arap_doc_botp_key') THEN
    ALTER TABLE bizfi_fi_arap_doc ADD UNIQUE KEY uk_bizfi_fi_arap_doc_botp_key (fbotp_idempotency_key);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'bizfi_fi_arap_doc' AND index_name = 'idx_bizfi_fi_arap_doc_botp_source') THEN
    ALTER TABLE bizfi_fi_arap_doc ADD INDEX idx_bizfi_fi_arap_doc_botp_source (fsource_system, fsource_document_type, fsource_document_id);
  END IF;
END$$

CALL bizfi_fi_core_repair()$$
DROP PROCEDURE IF EXISTS bizfi_fi_core_repair$$

DELIMITER ;

UPDATE bizfi_fi_arap_doc
SET fapplied_amount = COALESCE(fapplied_amount, 0),
    freserved_amount = COALESCE(freserved_amount, 0),
    fremaining_amount = CASE
        WHEN fdoctype = 'AP' THEN famount - COALESCE(fapplied_amount, 0) - COALESCE(freserved_amount, 0)
        ELSE fremaining_amount
    END,
    fpush_status = CASE
        WHEN fdoctype <> 'AP' THEN COALESCE(fpush_status, 'NOT_PUSHED')
        WHEN COALESCE(fapplied_amount, 0) + COALESCE(freserved_amount, 0) <= 0 THEN 'NOT_PUSHED'
        WHEN COALESCE(fapplied_amount, 0) + COALESCE(freserved_amount, 0) >= famount THEN 'COMPLETE'
        ELSE 'PARTIAL'
    END
WHERE fdoctype = 'AP' OR fdoctype = 'AP_PAYMENT_APPLY';
