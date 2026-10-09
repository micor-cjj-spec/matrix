USE matrix_erp;
CREATE TABLE IF NOT EXISTS matrix_erp_sales_workflow_link (
    fid BIGINT NOT NULL,
    ftenant_id VARCHAR(64) NOT NULL,
    forg_id BIGINT NOT NULL,
    fdocument_type VARCHAR(32) NOT NULL,
    fdocument_id BIGINT NOT NULL,
    fdefinition_key VARCHAR(100) NOT NULL,
    finitiator_id BIGINT NOT NULL,
    fidempotency_key VARCHAR(160) NOT NULL,
    finstance_id VARCHAR(40) NULL,
    fstatus VARCHAR(24) NOT NULL,
    fretry_count INT NOT NULL DEFAULT 0,
    fnext_retry_time DATETIME(3) NULL,
    fprocessed_event_id VARCHAR(40) NULL,
    fcreate_time DATETIME(3) NOT NULL,
    fmodify_time DATETIME(3) NOT NULL,
    PRIMARY KEY (fid),
    UNIQUE KEY uk_sales_workflow_document (ftenant_id, fdocument_type, fdocument_id),
    UNIQUE KEY uk_sales_workflow_instance (finstance_id),
    UNIQUE KEY uk_sales_workflow_idempotency (fidempotency_key),
    KEY idx_sales_workflow_pending (fstatus, fnext_retry_time, fmodify_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='销售审批与 Workflow 流程实例幂等关联';
