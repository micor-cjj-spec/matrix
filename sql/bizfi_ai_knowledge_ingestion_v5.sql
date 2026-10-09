-- Matrix Knowledge Phase 3B: file ingestion and asynchronous vector indexing
-- Apply after sql/bizfi_ai_knowledge_base_v4.sql.

CREATE TABLE IF NOT EXISTS bizfi_ai_knowledge_index_job (
    fid BIGINT PRIMARY KEY AUTO_INCREMENT,
    fjobid VARCHAR(64) NOT NULL,
    fkbid VARCHAR(64) NOT NULL,
    fdocid VARCHAR(128) NOT NULL,
    ffilename VARCHAR(255) NOT NULL,
    fmediatype VARCHAR(128),
    ffilesize BIGINT NOT NULL DEFAULT 0,
    fcontenthash VARCHAR(64) NOT NULL,
    fstatus VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    fattempts INT NOT NULL DEFAULT 0,
    fmaxattempts INT NOT NULL DEFAULT 3,
    ferrormessage VARCHAR(1000),
    fnextretrytime DATETIME,
    fstarttime DATETIME,
    ffinishtime DATETIME,
    fcreatetime DATETIME NOT NULL,
    fmodifytime DATETIME NOT NULL,
    UNIQUE KEY uk_knowledge_index_job_id (fjobid),
    KEY idx_knowledge_index_job_dispatch (fstatus, fnextretrytime, fid),
    KEY idx_knowledge_index_job_doc (fdocid, fcreatetime),
    KEY idx_knowledge_index_job_kb (fkbid, fcreatetime)
);
