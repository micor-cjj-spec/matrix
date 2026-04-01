ALTER TABLE bizfi_fi_counterparty_credit
  ADD COLUMN IF NOT EXISTS fblock_on_over_limit TINYINT NOT NULL DEFAULT 0 AFTER fenabled,
  ADD COLUMN IF NOT EXISTS fblock_on_overdue TINYINT NOT NULL DEFAULT 0 AFTER fblock_on_over_limit;
