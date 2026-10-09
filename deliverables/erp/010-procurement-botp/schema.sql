USE matrix_erp;

-- P1-IMP-07: Contract -> PurchaseOrder BOTP target idempotency.
ALTER TABLE matrix_erp_purchase_order
    ADD COLUMN fbotp_idempotency_key VARCHAR(255) NULL
        COMMENT 'BOTP目标创建幂等键' AFTER fclose_status,
    ADD COLUMN fsource_execution_id VARCHAR(64) NULL
        COMMENT '来源BOTP执行ID' AFTER fbotp_idempotency_key;

ALTER TABLE matrix_erp_purchase_order
    ADD UNIQUE KEY uk_matrix_erp_purchase_order_botp (
        ftenant_id,
        fbotp_idempotency_key
    ),
    ADD KEY idx_matrix_erp_purchase_order_source_execution (
        ftenant_id,
        fsource_execution_id
    );
