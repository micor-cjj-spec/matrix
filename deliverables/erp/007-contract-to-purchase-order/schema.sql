USE matrix_erp;

ALTER TABLE matrix_erp_purchase_order_entry
    ADD COLUMN fcontract_entry_id BIGINT NULL COMMENT '来源采购合同分录ID' AFTER fpurchase_order_id,
    ADD COLUMN fsourcing_award_entry_id BIGINT NULL COMMENT '来源定标分录ID' AFTER fcontract_entry_id,
    ADD COLUMN frfq_entry_id BIGINT NULL COMMENT '来源询价分录ID' AFTER fsourcing_award_entry_id,
    ADD COLUMN fpurchase_request_id BIGINT NULL COMMENT '来源采购申请ID' AFTER frfq_entry_id,
    ADD COLUMN fpurchase_request_entry_id BIGINT NULL COMMENT '来源采购申请分录ID' AFTER fpurchase_request_id,
    ADD KEY idx_matrix_erp_purchase_order_entry_contract_source (ftenant_id, fcontract_entry_id),
    ADD KEY idx_matrix_erp_purchase_order_entry_request_source (ftenant_id, fpurchase_request_id, fpurchase_request_entry_id);
