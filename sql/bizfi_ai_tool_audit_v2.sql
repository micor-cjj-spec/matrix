DROP PROCEDURE IF EXISTS migrate_bizfi_ai_tool_audit_v2;

DELIMITER //
CREATE PROCEDURE migrate_bizfi_ai_tool_audit_v2()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'bizfi_ai_tool_execution'
      AND COLUMN_NAME = 'fconversationid'
  ) THEN
    ALTER TABLE bizfi_ai_tool_execution
      ADD COLUMN fconversationid VARCHAR(64) NULL AFTER frequestid;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'bizfi_ai_tool_execution'
      AND COLUMN_NAME = 'fmodelname'
  ) THEN
    ALTER TABLE bizfi_ai_tool_execution
      ADD COLUMN fmodelname VARCHAR(128) NULL AFTER fconversationid;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'bizfi_ai_tool_execution'
      AND COLUMN_NAME = 'fmodeltraceid'
  ) THEN
    ALTER TABLE bizfi_ai_tool_execution
      ADD COLUMN fmodeltraceid VARCHAR(64) NULL AFTER fmodelname;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'bizfi_ai_tool_execution'
      AND INDEX_NAME = 'idx_ai_tool_conversation_time'
  ) THEN
    ALTER TABLE bizfi_ai_tool_execution
      ADD KEY idx_ai_tool_conversation_time (fconversationid, fcreatetime);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'bizfi_ai_tool_execution'
      AND INDEX_NAME = 'idx_ai_tool_model_trace'
  ) THEN
    ALTER TABLE bizfi_ai_tool_execution
      ADD KEY idx_ai_tool_model_trace (fmodeltraceid);
  END IF;
END//
DELIMITER ;

CALL migrate_bizfi_ai_tool_audit_v2();
DROP PROCEDURE IF EXISTS migrate_bizfi_ai_tool_audit_v2;
