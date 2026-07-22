-- Matrix OpenAPI V2
-- 第二阶段：治理、调用日志、租户/组织/账簿数据权限

ALTER TABLE bizfi_fi_voucher
    ADD COLUMN tenant_id VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '租户' AFTER fid,
    ADD COLUMN org_id VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '业务组织' AFTER tenant_id,
    ADD COLUMN book_id VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '账簿' AFTER org_id,
    ADD KEY idx_voucher_openapi_scope (tenant_id, org_id, book_id, fstatus, fdate),
    ADD KEY idx_voucher_openapi_number (tenant_id, fnumber);

UPDATE bizfi_fi_voucher
SET tenant_id = COALESCE(NULLIF(tenant_id, ''), 'default'),
    org_id = COALESCE(NULLIF(org_id, ''), 'default'),
    book_id = COALESCE(NULLIF(book_id, ''), 'default');

ALTER TABLE matrix_open_api_request_log
    ADD KEY idx_open_api_log_success_time (success, request_time),
    ADD KEY idx_open_api_log_code_time (response_code, request_time),
    ADD KEY idx_open_api_log_ip_time (client_ip, request_time);

-- 旧授权未配置组织/账簿时按租户内全范围处理。
-- 新授权建议显式写入：
-- {
--   "allowedStatuses": ["POSTED"],
--   "organizationIds": ["ORG-001"],
--   "bookIds": ["BOOK-001"],
--   "maxHistoryMonths": 24
-- }
