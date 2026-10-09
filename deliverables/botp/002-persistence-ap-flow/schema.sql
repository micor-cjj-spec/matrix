-- BOTP V2: 持久化 + 应付单部分金额下推付款申请单
-- 执行前请先备份 matrix_botp 与 matrix_fi。

USE matrix_botp;

ALTER TABLE matrix_botp_document_relation
    ADD COLUMN ftarget_document_no VARCHAR(128) NULL COMMENT '目标单号' AFTER ftarget_document_id,
    ADD COLUMN fallocated_amount DECIMAL(23, 10) NOT NULL DEFAULT 0 COMMENT '本次关联金额' AFTER ftarget_document_no,
    ADD UNIQUE KEY uk_matrix_botp_relation_execution_source_target
        (fexecution_id, fsource_document_id, ftarget_document_id, fdelete_flag);

-- 已发布规则版本保留 JSON 快照，草稿同样暂存在版本表，发布后状态切换为 PUBLISHED。
CREATE INDEX idx_matrix_botp_rule_version_latest
    ON matrix_botp_rule_version (ftenant_id, frule_code, fstatus, fversion_no);

USE matrix_fi;

ALTER TABLE bizfi_fi_arap_doc
    ADD COLUMN fapplied_amount DECIMAL(23, 10) NOT NULL DEFAULT 0 COMMENT 'BOTP 已生效申请金额',
    ADD COLUMN freserved_amount DECIMAL(23, 10) NOT NULL DEFAULT 0 COMMENT 'BOTP 下推预占金额',
    ADD COLUMN fremaining_amount DECIMAL(23, 10) NULL COMMENT 'BOTP 可下推剩余金额',
    ADD COLUMN fpush_status VARCHAR(32) NOT NULL DEFAULT 'NOT_PUSHED' COMMENT 'NOT_PUSHED/PARTIAL/COMPLETE',
    ADD COLUMN fbotp_idempotency_key VARCHAR(255) NULL COMMENT 'BOTP 目标创建幂等键',
    ADD COLUMN fsource_system VARCHAR(64) NULL COMMENT '来源系统',
    ADD COLUMN fsource_document_type VARCHAR(64) NULL COMMENT '来源单据类型',
    ADD COLUMN fsource_document_id VARCHAR(128) NULL COMMENT '来源单据ID',
    ADD COLUMN fsource_execution_id VARCHAR(64) NULL COMMENT '来源BOTP执行ID',
    ADD COLUMN fversion INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本';

CREATE UNIQUE INDEX uk_bizfi_fi_arap_doc_botp_key
    ON bizfi_fi_arap_doc (fbotp_idempotency_key);

CREATE INDEX idx_bizfi_fi_arap_doc_botp_source
    ON bizfi_fi_arap_doc (fsource_system, fsource_document_type, fsource_document_id);

UPDATE bizfi_fi_arap_doc
SET fapplied_amount = COALESCE(fapplied_amount, 0),
    freserved_amount = COALESCE(freserved_amount, 0),
    fremaining_amount = CASE
        WHEN fdoctype = 'AP' THEN famount - COALESCE(fapplied_amount, 0) - COALESCE(freserved_amount, 0)
        ELSE fremaining_amount
    END,
    fpush_status = CASE
        WHEN fdoctype <> 'AP' THEN COALESCE(fpush_status, 'NOT_PUSHED')
        WHEN COALESCE(fapplied_amount, 0) + COALESCE(freserved_amount, 0) <= 0 THEN 'NOT_PUSHED'
        WHEN COALESCE(fapplied_amount, 0) + COALESCE(freserved_amount, 0) >= famount THEN 'COMPLETE'
        ELSE 'PARTIAL'
    END
WHERE fdoctype = 'AP' OR fdoctype = 'AP_PAYMENT_APPLY';
