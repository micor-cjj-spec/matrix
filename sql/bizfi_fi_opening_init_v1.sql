CREATE TABLE IF NOT EXISTS bizfi_fi_subject_opening_balance (
    fid BIGINT PRIMARY KEY AUTO_INCREMENT,
    forg BIGINT NULL,
    fperiod VARCHAR(20) NOT NULL,
    faccount_id BIGINT NULL,
    faccount_code VARCHAR(64) NOT NULL,
    faccount_name VARCHAR(128) NOT NULL,
    fdebit_amount DECIMAL(18,2) DEFAULT 0,
    fcredit_amount DECIMAL(18,2) DEFAULT 0,
    fremark VARCHAR(255) NULL,
    fcreatetime DATETIME DEFAULT CURRENT_TIMESTAMP,
    fupdatetime DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS bizfi_fi_cashflow_opening (
    fid BIGINT PRIMARY KEY AUTO_INCREMENT,
    forg BIGINT NULL,
    fperiod VARCHAR(20) NOT NULL,
    fcashflow_item_id BIGINT NULL,
    fcashflow_code VARCHAR(64) NOT NULL,
    fcashflow_name VARCHAR(128) NOT NULL,
    famount DECIMAL(18,2) DEFAULT 0,
    fdirection VARCHAR(16) DEFAULT 'INFLOW',
    fremark VARCHAR(255) NULL,
    fcreatetime DATETIME DEFAULT CURRENT_TIMESTAMP,
    fupdatetime DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS bizfi_fi_counterparty_opening_balance (
    fid BIGINT PRIMARY KEY AUTO_INCREMENT,
    forg BIGINT NULL,
    fperiod VARCHAR(20) NOT NULL,
    fcounterparty_type VARCHAR(32) NOT NULL,
    fcounterparty_id BIGINT NULL,
    fcounterparty_code VARCHAR(64) NULL,
    fcounterparty_name VARCHAR(128) NOT NULL,
    faccount_code VARCHAR(64) NULL,
    faccount_name VARCHAR(128) NULL,
    famount DECIMAL(18,2) DEFAULT 0,
    fdirection VARCHAR(16) DEFAULT 'DEBIT',
    fremark VARCHAR(255) NULL,
    fcreatetime DATETIME DEFAULT CURRENT_TIMESTAMP,
    fupdatetime DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
