CREATE TABLE IF NOT EXISTS bizfi_ai_conversation (
  fid BIGINT PRIMARY KEY AUTO_INCREMENT,
  fconversationid VARCHAR(64) NOT NULL,
  fuserid BIGINT NOT NULL,
  ftitle VARCHAR(255) NOT NULL,
  fscene VARCHAR(64) DEFAULT 'knowledge_qa',
  fstatus VARCHAR(32) DEFAULT 'ACTIVE',
  fsource VARCHAR(32) DEFAULT 'WEB',
  fcreatetime DATETIME NOT NULL,
  fmodifytime DATETIME NOT NULL,
  UNIQUE KEY uk_fconversationid (fconversationid),
  KEY idx_fuserid (fuserid)
);

CREATE TABLE IF NOT EXISTS bizfi_ai_message (
  fid BIGINT PRIMARY KEY AUTO_INCREMENT,
  fconversationid VARCHAR(64) NOT NULL,
  frole VARCHAR(32) NOT NULL,
  fcontent LONGTEXT NOT NULL,
  fmodel VARCHAR(128),
  fmode VARCHAR(32),
  ftraceid VARCHAR(64),
  fprompttokens INT DEFAULT 0,
  fcompletiontokens INT DEFAULT 0,
  ftotaltokens INT DEFAULT 0,
  fcreatetime DATETIME NOT NULL,
  KEY idx_fconversationid (fconversationid)
);

CREATE TABLE IF NOT EXISTS bizfi_ai_feedback (
  fid BIGINT PRIMARY KEY AUTO_INCREMENT,
  fconversationid VARCHAR(64) NOT NULL,
  fmessageid BIGINT,
  fuserid BIGINT NOT NULL,
  ffeedbacktype VARCHAR(32) NOT NULL,
  fcomment VARCHAR(1000),
  fcreatetime DATETIME NOT NULL,
  KEY idx_fconversationid (fconversationid),
  KEY idx_fuserid (fuserid)
);

CREATE TABLE IF NOT EXISTS bizfi_ai_knowledge_doc (
  fid BIGINT PRIMARY KEY AUTO_INCREMENT,
  fdocid VARCHAR(128) NOT NULL,
  ftitle VARCHAR(255) NOT NULL,
  fcategory VARCHAR(64),
  fsourcepath VARCHAR(500) NOT NULL,
  fcontent LONGTEXT NOT NULL,
  fversion VARCHAR(64),
  fstatus VARCHAR(32) DEFAULT 'ACTIVE',
  fcreatetime DATETIME NOT NULL,
  fmodifytime DATETIME NOT NULL,
  UNIQUE KEY uk_fdocid (fdocid),
  KEY idx_status_category (fstatus, fcategory)
);

CREATE TABLE IF NOT EXISTS bizfi_ai_knowledge_chunk (
  fid BIGINT PRIMARY KEY AUTO_INCREMENT,
  fdocid VARCHAR(128) NOT NULL,
  fchunkid VARCHAR(160) NOT NULL,
  fseq INT NOT NULL,
  fcontent TEXT NOT NULL,
  fkeywords VARCHAR(1000),
  fcreatetime DATETIME NOT NULL,
  UNIQUE KEY uk_fchunkid (fchunkid),
  KEY idx_fdocid (fdocid)
);

ALTER TABLE bizfi_ai_knowledge_doc MODIFY COLUMN fdocid VARCHAR(128) NOT NULL;
ALTER TABLE bizfi_ai_knowledge_chunk MODIFY COLUMN fdocid VARCHAR(128) NOT NULL;
ALTER TABLE bizfi_ai_knowledge_chunk MODIFY COLUMN fchunkid VARCHAR(160) NOT NULL;
