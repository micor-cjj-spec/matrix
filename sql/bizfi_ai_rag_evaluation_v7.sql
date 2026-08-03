-- Matrix AI RAG Evaluation V7
-- Apply after knowledge migrations V4, V5 and V6.
-- Keep AI_KNOWLEDGE_EVALUATION_ENABLED=false until this migration has completed.

CREATE TABLE IF NOT EXISTS bizfi_ai_rag_eval_set (
    fid BIGINT PRIMARY KEY AUTO_INCREMENT,
    fsetid VARCHAR(64) NOT NULL,
    fkbid VARCHAR(64) NOT NULL,
    fname VARCHAR(160) NOT NULL,
    fdescription VARCHAR(1000) NULL,
    fstatus VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    fcreatedby BIGINT NOT NULL,
    fcreatetime DATETIME NOT NULL,
    fmodifytime DATETIME NOT NULL,
    UNIQUE KEY uk_ai_rag_eval_set_id (fsetid),
    KEY idx_ai_rag_eval_set_kb_status (fkbid, fstatus, fmodifytime)
);

CREATE TABLE IF NOT EXISTS bizfi_ai_rag_eval_case (
    fid BIGINT PRIMARY KEY AUTO_INCREMENT,
    fcaseid VARCHAR(64) NOT NULL,
    fsetid VARCHAR(64) NOT NULL,
    fquestion TEXT NOT NULL,
    fexpecteddocids TEXT NULL,
    fexpectedchunkids TEXT NULL,
    ftopk INT NOT NULL DEFAULT 5,
    fstatus VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    fcreatetime DATETIME NOT NULL,
    fmodifytime DATETIME NOT NULL,
    UNIQUE KEY uk_ai_rag_eval_case_id (fcaseid),
    KEY idx_ai_rag_eval_case_set_status (fsetid, fstatus, fid)
);

CREATE TABLE IF NOT EXISTS bizfi_ai_rag_eval_run (
    fid BIGINT PRIMARY KEY AUTO_INCREMENT,
    frunid VARCHAR(64) NOT NULL,
    fsetid VARCHAR(64) NOT NULL,
    fkbid VARCHAR(64) NOT NULL,
    fstatus VARCHAR(32) NOT NULL,
    fcasecount INT NOT NULL DEFAULT 0,
    fcompletedcount INT NOT NULL DEFAULT 0,
    fhitcount INT NOT NULL DEFAULT 0,
    fhitatk DECIMAL(12, 6) NULL,
    fmrr DECIMAL(12, 6) NULL,
    frecallatk DECIMAL(12, 6) NULL,
    favglatencyms DECIMAL(16, 3) NULL,
    fp95latencyms BIGINT NULL,
    fconfigsnapshot TEXT NULL,
    ferrormessage VARCHAR(1000) NULL,
    fcreatedby BIGINT NOT NULL,
    fstarttime DATETIME NULL,
    ffinishtime DATETIME NULL,
    fcreatetime DATETIME NOT NULL,
    fmodifytime DATETIME NOT NULL,
    UNIQUE KEY uk_ai_rag_eval_run_id (frunid),
    KEY idx_ai_rag_eval_run_set_time (fsetid, fcreatetime),
    KEY idx_ai_rag_eval_run_status_time (fstatus, fcreatetime)
);

CREATE TABLE IF NOT EXISTS bizfi_ai_rag_eval_result (
    fid BIGINT PRIMARY KEY AUTO_INCREMENT,
    frunid VARCHAR(64) NOT NULL,
    fcaseid VARCHAR(64) NOT NULL,
    fquestion TEXT NOT NULL,
    fexpecteddocids TEXT NULL,
    fexpectedchunkids TEXT NULL,
    fretrieveddocids TEXT NULL,
    fretrievedchunkids TEXT NULL,
    fhit TINYINT(1) NOT NULL DEFAULT 0,
    ffirstrelevantrank INT NULL,
    freciprocalrank DECIMAL(12, 6) NOT NULL DEFAULT 0,
    frecallatk DECIMAL(12, 6) NOT NULL DEFAULT 0,
    flatencyms BIGINT NOT NULL DEFAULT 0,
    ferrormessage VARCHAR(1000) NULL,
    fcreatetime DATETIME NOT NULL,
    UNIQUE KEY uk_ai_rag_eval_result_case (frunid, fcaseid),
    KEY idx_ai_rag_eval_result_run_rank (frunid, ffirstrelevantrank),
    KEY idx_ai_rag_eval_result_run_hit (frunid, fhit)
);
