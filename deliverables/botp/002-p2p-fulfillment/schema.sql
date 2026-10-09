USE matrix_botp;

-- P0-IMP-02：关系幂等由数据库兜底，避免多实例 pre-check + insert 竞态。
ALTER TABLE matrix_botp_document_relation
    ADD UNIQUE KEY uk_matrix_botp_relation_identity (
        ftenant_id,
        fexecution_id,
        fsource_system_code,
        fsource_document_type,
        fsource_document_id,
        ftarget_system_code,
        ftarget_document_type,
        ftarget_document_id,
        fdelete_flag
    );

ALTER TABLE matrix_botp_document_relation_entry
    ADD UNIQUE KEY uk_matrix_botp_relation_entry_identity (
        frelation_id,
        fsource_entry_id,
        ftarget_entry_id,
        fdelete_flag
    );

-- 说明：matrix_botp_document_relation_entry 已在 001-document-conversion/schema.sql 中创建。
-- 本阶段开始由 Java Repository 正式持久化和查询该表。
