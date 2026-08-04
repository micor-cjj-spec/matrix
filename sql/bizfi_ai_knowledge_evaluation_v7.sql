-- Matrix AI Knowledge Evaluation V7
-- Apply after the knowledge-base, ingestion, and ACL migrations.
-- This migration introduces persistent retrieval evaluation datasets, questions, runs, and per-question results.

CREATE TABLE IF NOT EXISTS bizfi_ai_evaluation_dataset (
    fid BIGINT PRIMARY KEY AUTO_INCREMENT,
    fdatasetid VARCHAR(64) NOT NULL,
    fname VARCHAR(255) NOT NULL,
    fdescription TEXT NULL,
    fstatus VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    fcreatetime DATETIME NOT NULL,
    fmodifytime DATETIME NOT NULL,
    UNIQUE KEY uk_ai_evaluation_dataset_id (fdatasetid),
    KEY idx_ai_evaluation_dataset_status (fstatus, fmodifytime)
);

CREATE TABLE IF NOT EXISTS bizfi_ai_evaluation_question (
    fid BIGINT PRIMARY KEY AUTO_INCREMENT,
    fquestionid VARCHAR(64) NOT NULL,
    fdatasetid VARCHAR(64) NOT NULL,
    fquestion TEXT NOT NULL,
    fkbids TEXT NULL,
    fexpecteddocids TEXT NULL,
    fexpectedchunkids TEXT NULL,
    fexpectedanswer LONGTEXT NULL,
    fstatus VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    fcreatetime DATETIME NOT NULL,
    fmodifytime DATETIME NOT NULL,
    UNIQUE KEY uk_ai_evaluation_question_id (fquestionid),
    KEY idx_ai_evaluation_question_dataset (fdatasetid, fstatus, fid)
);

CREATE TABLE IF NOT EXISTS bizfi_ai_evaluation_run (
    fid BIGINT PRIMARY KEY AUTO_INCREMENT,
    frunid VARCHAR(64) NOT NULL,
    fdatasetid VARCHAR(64) NOT NULL,
    fstatus VARCHAR(32) NOT NULL,
    ftopk INT NOT NULL,
    ftotalquestions INT NOT NULL DEFAULT 0,
    fcompletedquestions INT NOT NULL DEFAULT 0,
    frecallatk DECIMAL(10, 6) NULL,
    fmrr DECIMAL(10, 6) NULL,
    fzerohitrate DECIMAL(10, 6) NULL,
    favglatencyms BIGINT NULL,
    ferrormessage VARCHAR(1000) NULL,
    fstarttime DATETIME NULL,
    ffinishtime DATETIME NULL,
    fcreatetime DATETIME NOT NULL,
    fmodifytime DATETIME NOT NULL,
    UNIQUE KEY uk_ai_evaluation_run_id (frunid),
    KEY idx_ai_evaluation_run_dataset (fdatasetid, fcreatetime),
    KEY idx_ai_evaluation_run_status (fstatus, fmodifytime)
);

CREATE TABLE IF NOT EXISTS bizfi_ai_evaluation_result (
    fid BIGINT PRIMARY KEY AUTO_INCREMENT,
    fresultid VARCHAR(64) NOT NULL,
    frunid VARCHAR(64) NOT NULL,
    fquestionid VARCHAR(64) NOT NULL,
    fcitationsjson LONGTEXT NULL,
    ffirstrelevantrank INT NULL,
    frecall DECIMAL(10, 6) NOT NULL DEFAULT 0,
    freciprocalrank DECIMAL(10, 6) NOT NULL DEFAULT 0,
    flatencyms BIGINT NOT NULL DEFAULT 0,
    ferrormessage VARCHAR(1000) NULL,
    fcreatetime DATETIME NOT NULL,
    UNIQUE KEY uk_ai_evaluation_result_id (fresultid),
    UNIQUE KEY uk_ai_evaluation_result_run_question (frunid, fquestionid),
    KEY idx_ai_evaluation_result_run (frunid, fid)
);
