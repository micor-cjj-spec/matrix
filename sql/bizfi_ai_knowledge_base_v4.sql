-- Matrix AI Knowledge Base V4
-- Apply after sql/bizfi_ai_v1.sql.

CREATE TABLE IF NOT EXISTS bizfi_ai_knowledge_base (
    fid BIGINT PRIMARY KEY AUTO_INCREMENT,
    fkbid VARCHAR(64) NOT NULL,
    fname VARCHAR(128) NOT NULL,
    fdescription VARCHAR(500),
    fstatus VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    fcreatetime DATETIME NOT NULL,
    fmodifytime DATETIME NOT NULL,
    UNIQUE KEY uk_ai_knowledge_base_kbid (fkbid),
    KEY idx_ai_knowledge_base_status (fstatus, fmodifytime)
);

INSERT IGNORE INTO bizfi_ai_knowledge_base (
    fkbid, fname, fdescription, fstatus, fcreatetime, fmodifytime
) VALUES (
    'default', '默认知识库', '系统默认知识库，承接升级前已有知识文档。', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

ALTER TABLE bizfi_ai_knowledge_doc
    ADD COLUMN fkbid VARCHAR(64) NOT NULL DEFAULT 'default' AFTER fid,
    ADD INDEX idx_ai_knowledge_doc_kb_status (fkbid, fstatus, fmodifytime);

UPDATE bizfi_ai_knowledge_doc
SET fkbid = 'default'
WHERE fkbid IS NULL OR fkbid = '';
