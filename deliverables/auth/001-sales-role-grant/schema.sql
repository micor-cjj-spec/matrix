-- Execute in the database configured for auth-service. New role grants must be provisioned
-- by authorized administrators only; never insert privileged demo users automatically.
CREATE TABLE IF NOT EXISTS matrix_auth_sales_role_grant (
    fid BIGINT NOT NULL,
    ftenant_id VARCHAR(64) NOT NULL,
    forg_id BIGINT NOT NULL,
    fuser_id BIGINT NOT NULL,
    frole_code VARCHAR(64) NOT NULL,
    fstatus VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    fcreate_by BIGINT NULL,
    fcreate_time DATETIME NOT NULL,
    fmodify_by BIGINT NULL,
    fmodify_time DATETIME NULL,
    fdelete_flag TINYINT NOT NULL DEFAULT 0,
    fversion INT NOT NULL DEFAULT 0,
    PRIMARY KEY (fid),
    UNIQUE KEY uk_matrix_auth_sales_grant (ftenant_id, forg_id, fuser_id, frole_code, fdelete_flag),
    KEY idx_matrix_auth_sales_grant_user (ftenant_id, fuser_id, fstatus),
    KEY idx_matrix_auth_sales_grant_org (ftenant_id, forg_id, frole_code)
) COMMENT='经授权管理员维护的销售角色授权';

