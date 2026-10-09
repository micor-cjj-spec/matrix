-- Matrix AI Knowledge ACL V6
-- Apply after sql/bizfi_ai_knowledge_base_v4.sql and sql/bizfi_ai_knowledge_ingestion_v5.sql.
-- Keep AI_KNOWLEDGE_ACL_ENABLED=false until existing knowledge bases have at least one OWNER or ADMIN grant.

CREATE TABLE IF NOT EXISTS bizfi_ai_knowledge_base_acl (
    fid BIGINT PRIMARY KEY AUTO_INCREMENT,
    fkbid VARCHAR(64) NOT NULL,
    fsubjecttype VARCHAR(32) NOT NULL,
    fsubjectid VARCHAR(128) NOT NULL,
    fpermission VARCHAR(32) NOT NULL,
    fcreatedby BIGINT NOT NULL,
    fcreatetime DATETIME NOT NULL,
    fmodifytime DATETIME NOT NULL,
    UNIQUE KEY uk_ai_kb_acl_subject (fkbid, fsubjecttype, fsubjectid),
    KEY idx_ai_kb_acl_subject_lookup (fsubjecttype, fsubjectid, fpermission),
    KEY idx_ai_kb_acl_kb_permission (fkbid, fpermission)
);

-- Bootstrap examples. Replace values before enabling ACL.
-- User owner:
-- INSERT INTO bizfi_ai_knowledge_base_acl
-- (fkbid, fsubjecttype, fsubjectid, fpermission, fcreatedby, fcreatetime, fmodifytime)
-- VALUES ('default', 'USER', '10001', 'OWNER', 10001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
-- ON DUPLICATE KEY UPDATE fpermission = VALUES(fpermission), fmodifytime = CURRENT_TIMESTAMP;

-- Organization viewer:
-- INSERT INTO bizfi_ai_knowledge_base_acl
-- (fkbid, fsubjecttype, fsubjectid, fpermission, fcreatedby, fcreatetime, fmodifytime)
-- VALUES ('default', 'ORGANIZATION', '1', 'VIEWER', 10001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
-- ON DUPLICATE KEY UPDATE fpermission = VALUES(fpermission), fmodifytime = CURRENT_TIMESTAMP;
