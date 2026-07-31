-- Idempotent fix for voucher OpenAPI compatibility columns (MySQL 8).
-- Keep bizfi_fi_voucher aligned with BizfiFiVoucher fields used by fi-service.
-- Safe to rerun.

USE bizfi_fi;
SET @db := DATABASE();
SET @table := 'bizfi_fi_voucher';

SET @sql := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@db AND table_name=@table AND column_name='tenant_id')=0,
  'ALTER TABLE bizfi_fi_voucher ADD COLUMN tenant_id VARCHAR(64) NULL COMMENT ''Tenant ID'' AFTER fid', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@db AND table_name=@table AND column_name='org_id')=0,
  'ALTER TABLE bizfi_fi_voucher ADD COLUMN org_id VARCHAR(64) NULL COMMENT ''Organization ID'' AFTER tenant_id', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@db AND table_name=@table AND column_name='book_id')=0,
  'ALTER TABLE bizfi_fi_voucher ADD COLUMN book_id VARCHAR(64) NULL COMMENT ''Book ID'' AFTER org_id', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@db AND table_name=@table AND column_name='source_request_id')=0,
  'ALTER TABLE bizfi_fi_voucher ADD COLUMN source_request_id VARCHAR(128) NULL COMMENT ''OpenAPI source request'' AFTER book_id', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF((SELECT CHARACTER_MAXIMUM_LENGTH FROM information_schema.columns WHERE table_schema=@db AND table_name=@table AND column_name='source_request_id') < 128,
  'ALTER TABLE bizfi_fi_voucher MODIFY COLUMN source_request_id VARCHAR(128) NULL COMMENT ''OpenAPI source request''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=@db AND table_name=@table AND index_name='idx_voucher_source_request')=0,
  'ALTER TABLE bizfi_fi_voucher ADD INDEX idx_voucher_source_request (source_request_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
