-- Matrix AI Knowledge Evaluation Trace V8
-- Apply after sql/bizfi_ai_knowledge_evaluation_v7.sql.
-- Stores immutable per-question retrieval candidates, RRF scores, backend routing,
-- fallback diagnostics, embedding model, and retrieval configuration fingerprint.

CREATE TABLE IF NOT EXISTS bizfi_ai_evaluation_trace (
    fid BIGINT PRIMARY KEY AUTO_INCREMENT,
    ftraceid VARCHAR(64) NOT NULL,
    frunid VARCHAR(64) NOT NULL,
    fresultid VARCHAR(64) NOT NULL,
    fquestionid VARCHAR(64) NOT NULL,
    fconfigfingerprint VARCHAR(64) NOT NULL,
    fmode VARCHAR(32) NOT NULL,
    ftracejson LONGTEXT NOT NULL,
    fcreatetime DATETIME NOT NULL,
    UNIQUE KEY uk_ai_evaluation_trace_id (ftraceid),
    UNIQUE KEY uk_ai_evaluation_trace_result (fresultid),
    UNIQUE KEY uk_ai_evaluation_trace_run_question (frunid, fquestionid),
    KEY idx_ai_evaluation_trace_run (frunid, fid),
    KEY idx_ai_evaluation_trace_config (fconfigfingerprint, fcreatetime)
);
