DELIMITER $$

DROP PROCEDURE IF EXISTS bizfi_fi_repair_cashflow_link_columns$$
CREATE PROCEDURE bizfi_fi_repair_cashflow_link_columns()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'bizfi_fi_voucher_line'
      AND column_name = 'fcashflow_item'
  ) THEN
    ALTER TABLE bizfi_fi_voucher_line
      ADD COLUMN fcashflow_item VARCHAR(64) NULL COMMENT 'Cash-flow item code';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'bizfi_fi_gl_entry'
      AND column_name = 'fcashflow_item'
  ) THEN
    ALTER TABLE bizfi_fi_gl_entry
      ADD COLUMN fcashflow_item VARCHAR(64) NULL COMMENT 'Cash-flow item code';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'bizfi_fi_voucher_line'
      AND index_name = 'idx_voucher_line_cashflow_item'
  ) THEN
    ALTER TABLE bizfi_fi_voucher_line ADD INDEX idx_voucher_line_cashflow_item (fcashflow_item);
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'bizfi_fi_gl_entry'
      AND index_name = 'idx_gl_entry_cashflow_item'
  ) THEN
    ALTER TABLE bizfi_fi_gl_entry ADD INDEX idx_gl_entry_cashflow_item (fcashflow_item);
  END IF;
END$$

CALL bizfi_fi_repair_cashflow_link_columns()$$
DROP PROCEDURE IF EXISTS bizfi_fi_repair_cashflow_link_columns$$

DELIMITER ;
