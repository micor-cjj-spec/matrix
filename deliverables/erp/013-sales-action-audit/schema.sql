USE matrix_erp;

-- Append-only audit for sales actions. No public update or delete endpoint.
CREATE TABLE IF NOT EXISTS matrix_erp_sales_action_audit (
    fid BIGINT NOT NULL,
    ftenant_id VARCHAR(64) NOT NULL,
    forg_id BIGINT NOT NULL,
    fdocument_type VARCHAR(32) NOT NULL,
    fdocument_id BIGINT NOT NULL,
    faction VARCHAR(32) NOT NULL,
    fbefore_status VARCHAR(64) NULL,
    fafter_status VARCHAR(64) NOT NULL,
    foperator_id BIGINT NOT NULL,
    fcreate_time DATETIME(3) NOT NULL,
    PRIMARY KEY (fid),
    KEY idx_matrix_erp_sales_audit_business
        (ftenant_id, forg_id, fdocument_type, fdocument_id, fcreate_time, fid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='销售单据操作审计（只追加）';
