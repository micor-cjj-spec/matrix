CREATE TABLE IF NOT EXISTS bizfi_ai_audit_access_log (
  fid BIGINT PRIMARY KEY AUTO_INCREMENT,
  faccessrequestid VARCHAR(64) NOT NULL,
  foperatorid VARCHAR(64) NOT NULL,
  foperatorroles VARCHAR(256) NOT NULL,
  faction VARCHAR(32) NOT NULL,
  ffiltersummary VARCHAR(500) NOT NULL,
  foutcome VARCHAR(32) NOT NULL,
  fresultcount BIGINT NOT NULL DEFAULT 0,
  fdurationms BIGINT NOT NULL DEFAULT 0,
  ferrorcode VARCHAR(64),
  fcreatetime DATETIME NOT NULL,
  UNIQUE KEY uk_ai_audit_access_request (faccessrequestid),
  KEY idx_ai_audit_operator_time (foperatorid, fcreatetime),
  KEY idx_ai_audit_action_time (faction, fcreatetime)
);

DROP PROCEDURE IF EXISTS migrate_bizfi_ai_tool_audit_v3;

DELIMITER //
CREATE PROCEDURE migrate_bizfi_ai_tool_audit_v3()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'bizfi_ai_tool_execution'
      AND INDEX_NAME = 'idx_ai_tool_started_timeout'
  ) THEN
    ALTER TABLE bizfi_ai_tool_execution
      ADD KEY idx_ai_tool_started_timeout (fstatus, fstarttime);
  END IF;
END//
DELIMITER ;

CALL migrate_bizfi_ai_tool_audit_v3();
DROP PROCEDURE IF EXISTS migrate_bizfi_ai_tool_audit_v3;
