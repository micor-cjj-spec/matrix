-- BOTP V3: 反向反写、失败补偿、执行日志与自动对账
-- 执行前请先备份 matrix_botp 与 matrix_fi，并确保已执行 001、002 脚本。

USE matrix_botp;

ALTER TABLE matrix_botp_document_relation
    ADD COLUMN ftarget_status VARCHAR(32) NULL COMMENT '目标单当前状态' AFTER frelation_status,
    ADD COLUMN flast_event_id VARCHAR(128) NULL COMMENT '最后处理的生命周期事件ID' AFTER ftarget_status,
    ADD COLUMN finvalid_reason VARCHAR(500) NULL COMMENT '关系失效原因' AFTER flast_event_id,
    ADD COLUMN finvalid_time DATETIME NULL COMMENT '关系失效时间' AFTER finvalid_reason,
    ADD COLUMN freversed_time DATETIME NULL COMMENT '反向反写完成时间' AFTER finvalid_time;

CREATE INDEX idx_matrix_botp_relation_event
    ON matrix_botp_document_relation (ftenant_id, flast_event_id);

ALTER TABLE matrix_botp_writeback_task
    ADD COLUMN ftarget_system_code VARCHAR(64) NULL COMMENT '目标系统' AFTER fsource_document_id,
    ADD COLUMN ftarget_document_type VARCHAR(64) NULL COMMENT '目标单类型' AFTER ftarget_system_code,
    ADD COLUMN ftarget_document_id VARCHAR(128) NULL COMMENT '目标单ID' AFTER ftarget_document_type,
    ADD COLUMN ftarget_document_no VARCHAR(128) NULL COMMENT '目标单号' AFTER ftarget_document_id,
    ADD COLUMN ftask_type VARCHAR(32) NOT NULL DEFAULT 'FORWARD_WRITEBACK' COMMENT 'FORWARD/REVERSE/RECOMPUTE' AFTER ftarget_document_no,
    ADD COLUMN factive_allocated_amount DECIMAL(23, 10) NOT NULL DEFAULT 0 COMMENT '有效关系汇总金额' AFTER fstatus,
    ADD COLUMN frelease_reserved_amount DECIMAL(23, 10) NOT NULL DEFAULT 0 COMMENT '本次释放预占金额' AFTER factive_allocated_amount,
    ADD COLUMN ffinish_time DATETIME NULL COMMENT '任务完成时间' AFTER ferror_message;

CREATE INDEX idx_matrix_botp_writeback_relation
    ON matrix_botp_writeback_task (frelation_id, ftask_type, fstatus);

CREATE TABLE matrix_botp_execution_log (
    fid BIGINT NOT NULL COMMENT '主键',
    fexecution_id VARCHAR(64) NOT NULL COMMENT '执行ID',
    fstage VARCHAR(64) NOT NULL COMMENT '执行阶段',
    fstatus VARCHAR(32) NOT NULL COMMENT 'PROCESSING/SUCCEEDED/FAILED/DEAD',
    fmessage VARCHAR(1000) NULL COMMENT '阶段说明',
    frequest_snapshot LONGTEXT NULL COMMENT '请求摘要',
    fresponse_snapshot LONGTEXT NULL COMMENT '响应摘要',
    fexception_type VARCHAR(255) NULL COMMENT '异常类型',
    fstart_time DATETIME NOT NULL COMMENT '开始时间',
    ffinish_time DATETIME NULL COMMENT '完成时间',
    fcreate_time DATETIME NOT NULL COMMENT '创建时间',
    fdelete_flag TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (fid),
    KEY idx_matrix_botp_execution_log_execution (fexecution_id, fcreate_time),
    KEY idx_matrix_botp_execution_log_stage (fstage, fstatus, fcreate_time)
) COMMENT='BOTP执行阶段日志';

CREATE TABLE matrix_botp_reconciliation_issue (
    fid BIGINT NOT NULL COMMENT '主键',
    ftenant_id VARCHAR(64) NOT NULL COMMENT '租户ID',
    fissue_type VARCHAR(64) NOT NULL COMMENT '异常类型',
    fstatus VARCHAR(32) NOT NULL COMMENT 'OPEN/FIXED/IGNORED',
    fexecution_id VARCHAR(64) NULL COMMENT '执行ID',
    frelation_id BIGINT NULL COMMENT '关系ID',
    fsource_system_code VARCHAR(64) NULL COMMENT '源系统',
    fsource_document_type VARCHAR(64) NULL COMMENT '源单类型',
    fsource_document_id VARCHAR(128) NULL COMMENT '源单ID',
    ftarget_system_code VARCHAR(64) NULL COMMENT '目标系统',
    ftarget_document_type VARCHAR(64) NULL COMMENT '目标单类型',
    ftarget_document_id VARCHAR(128) NULL COMMENT '目标单ID',
    ftarget_document_no VARCHAR(128) NULL COMMENT '目标单号',
    fexpected_amount DECIMAL(23, 10) NULL COMMENT '期望金额',
    factual_amount DECIMAL(23, 10) NULL COMMENT '实际金额',
    fdescription VARCHAR(1000) NULL COMMENT '异常描述',
    fresolution VARCHAR(1000) NULL COMMENT '处理说明',
    fdetected_time DATETIME NOT NULL COMMENT '发现时间',
    fresolved_time DATETIME NULL COMMENT '解决时间',
    fcreate_time DATETIME NOT NULL COMMENT '创建时间',
    fmodify_time DATETIME NULL COMMENT '修改时间',
    fdelete_flag TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    fversion INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (fid),
    KEY idx_matrix_botp_reconciliation_open (fstatus, fissue_type, fdetected_time),
    KEY idx_matrix_botp_reconciliation_source (ftenant_id, fsource_document_id, fstatus),
    KEY idx_matrix_botp_reconciliation_target (ftenant_id, ftarget_document_id, fstatus)
) COMMENT='BOTP自动对账异常';
