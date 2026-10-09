CREATE DATABASE IF NOT EXISTS matrix_base
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE matrix_base;

CREATE TABLE IF NOT EXISTS matrix_base_business_partner (
    fid BIGINT NOT NULL COMMENT '主键',
    ftenant_id VARCHAR(64) NOT NULL COMMENT '租户ID',
    fcode VARCHAR(64) NOT NULL COMMENT '客商统一编码',
    fname VARCHAR(255) NOT NULL COMMENT '客商名称',
    fpartner_type VARCHAR(32) NOT NULL DEFAULT 'ORGANIZATION' COMMENT '主体类型',
    funified_social_credit_code VARCHAR(64) NULL COMMENT '统一社会信用代码',
    fsource_system VARCHAR(64) NULL COMMENT '来源系统',
    fsource_id VARCHAR(128) NULL COMMENT '来源对象ID',
    fstatus VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '主体生命周期',
    fapproval_status VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '审批状态',
    fcreate_by BIGINT NULL COMMENT '创建人',
    fcreate_time DATETIME NOT NULL COMMENT '创建时间',
    fmodify_by BIGINT NULL COMMENT '修改人',
    fmodify_time DATETIME NULL COMMENT '修改时间',
    fdelete_flag TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    fversion INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (fid),
    UNIQUE KEY uk_matrix_base_partner_code (ftenant_id, fcode, fdelete_flag),
    UNIQUE KEY uk_matrix_base_partner_credit (ftenant_id, funified_social_credit_code, fdelete_flag),
    KEY idx_matrix_base_partner_status (ftenant_id, fstatus, fapproval_status),
    KEY idx_matrix_base_partner_source (ftenant_id, fsource_system, fsource_id)
) COMMENT='统一BusinessPartner主体';

CREATE TABLE IF NOT EXISTS matrix_base_business_partner_role (
    fid BIGINT NOT NULL COMMENT '主键',
    ftenant_id VARCHAR(64) NOT NULL COMMENT '租户ID',
    fbusiness_partner_id BIGINT NOT NULL COMMENT 'BusinessPartner ID',
    frole_type VARCHAR(32) NOT NULL COMMENT 'CUSTOMER/SUPPLIER等角色',
    fstatus VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '角色状态',
    fcreate_by BIGINT NULL COMMENT '创建人',
    fcreate_time DATETIME NOT NULL COMMENT '创建时间',
    fmodify_by BIGINT NULL COMMENT '修改人',
    fmodify_time DATETIME NULL COMMENT '修改时间',
    fdelete_flag TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    fversion INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (fid),
    UNIQUE KEY uk_matrix_base_partner_role (
        ftenant_id, fbusiness_partner_id, frole_type, fdelete_flag
    ),
    KEY idx_matrix_base_partner_role_type (ftenant_id, frole_type, fstatus)
) COMMENT='BusinessPartner业务角色';

CREATE TABLE IF NOT EXISTS matrix_base_business_partner_org (
    fid BIGINT NOT NULL COMMENT '主键',
    ftenant_id VARCHAR(64) NOT NULL COMMENT '租户ID',
    fbusiness_partner_id BIGINT NOT NULL COMMENT 'BusinessPartner ID',
    forg_id BIGINT NOT NULL COMMENT '组织ID',
    fcustomer_enabled TINYINT NOT NULL DEFAULT 0 COMMENT '客户角色在组织可用',
    fsupplier_enabled TINYINT NOT NULL DEFAULT 0 COMMENT '供应商角色在组织可用',
    fstatus VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
    feffective_date DATE NULL COMMENT '生效日期',
    fexpiry_date DATE NULL COMMENT '失效日期',
    fcreate_by BIGINT NULL,
    fcreate_time DATETIME NOT NULL,
    fmodify_by BIGINT NULL,
    fmodify_time DATETIME NULL,
    fdelete_flag TINYINT NOT NULL DEFAULT 0,
    fversion INT NOT NULL DEFAULT 0,
    PRIMARY KEY (fid),
    UNIQUE KEY uk_matrix_base_partner_org (
        ftenant_id, fbusiness_partner_id, forg_id, fdelete_flag
    )
) COMMENT='BusinessPartner组织可用范围';

CREATE TABLE IF NOT EXISTS matrix_base_business_partner_contact (
    fid BIGINT NOT NULL,
    ftenant_id VARCHAR(64) NOT NULL,
    fbusiness_partner_id BIGINT NOT NULL,
    fname VARCHAR(128) NOT NULL,
    fcontact_type VARCHAR(32) NULL,
    fphone VARCHAR(64) NULL,
    femail VARCHAR(128) NULL,
    fis_primary TINYINT NOT NULL DEFAULT 0,
    fstatus VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
    fcreate_by BIGINT NULL,
    fcreate_time DATETIME NOT NULL,
    fmodify_by BIGINT NULL,
    fmodify_time DATETIME NULL,
    fdelete_flag TINYINT NOT NULL DEFAULT 0,
    fversion INT NOT NULL DEFAULT 0,
    PRIMARY KEY (fid),
    KEY idx_matrix_base_partner_contact (ftenant_id, fbusiness_partner_id, fstatus)
) COMMENT='BusinessPartner联系人';

CREATE TABLE IF NOT EXISTS matrix_base_business_partner_address (
    fid BIGINT NOT NULL,
    ftenant_id VARCHAR(64) NOT NULL,
    fbusiness_partner_id BIGINT NOT NULL,
    faddress_type VARCHAR(32) NULL,
    fcountry_code VARCHAR(32) NULL,
    fprovince VARCHAR(128) NULL,
    fcity VARCHAR(128) NULL,
    fdistrict VARCHAR(128) NULL,
    fdetail_address VARCHAR(500) NULL,
    fpostal_code VARCHAR(32) NULL,
    fis_default TINYINT NOT NULL DEFAULT 0,
    fstatus VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
    fcreate_by BIGINT NULL,
    fcreate_time DATETIME NOT NULL,
    fmodify_by BIGINT NULL,
    fmodify_time DATETIME NULL,
    fdelete_flag TINYINT NOT NULL DEFAULT 0,
    fversion INT NOT NULL DEFAULT 0,
    PRIMARY KEY (fid),
    KEY idx_matrix_base_partner_address (ftenant_id, fbusiness_partner_id, faddress_type)
) COMMENT='BusinessPartner地址';

CREATE TABLE IF NOT EXISTS matrix_base_business_partner_bank_account (
    fid BIGINT NOT NULL,
    ftenant_id VARCHAR(64) NOT NULL,
    fbusiness_partner_id BIGINT NOT NULL,
    fbusiness_partner_role_id BIGINT NULL,
    faccount_name VARCHAR(255) NOT NULL,
    fbank_code VARCHAR(64) NULL,
    fbank_name VARCHAR(255) NOT NULL,
    fbank_branch VARCHAR(255) NULL,
    faccount_no VARCHAR(128) NOT NULL,
    fcurrency_id BIGINT NULL,
    fusage_type VARCHAR(32) NOT NULL DEFAULT 'BOTH',
    fis_default TINYINT NOT NULL DEFAULT 0,
    fverification_status VARCHAR(32) NOT NULL DEFAULT 'UNVERIFIED',
    fstatus VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
    fcreate_by BIGINT NULL,
    fcreate_time DATETIME NOT NULL,
    fmodify_by BIGINT NULL,
    fmodify_time DATETIME NULL,
    fdelete_flag TINYINT NOT NULL DEFAULT 0,
    fversion INT NOT NULL DEFAULT 0,
    PRIMARY KEY (fid),
    KEY idx_matrix_base_partner_bank (ftenant_id, fbusiness_partner_id, fusage_type)
) COMMENT='BusinessPartner银行账户';

CREATE TABLE IF NOT EXISTS matrix_base_business_partner_tax_profile (
    fid BIGINT NOT NULL,
    ftenant_id VARCHAR(64) NOT NULL,
    fbusiness_partner_id BIGINT NOT NULL,
    ftax_no VARCHAR(64) NULL,
    ftaxpayer_type VARCHAR(32) NULL,
    finvoice_title VARCHAR(255) NULL,
    finvoice_address_phone VARCHAR(500) NULL,
    finvoice_bank_account VARCHAR(500) NULL,
    fstatus VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
    fcreate_by BIGINT NULL,
    fcreate_time DATETIME NOT NULL,
    fmodify_by BIGINT NULL,
    fmodify_time DATETIME NULL,
    fdelete_flag TINYINT NOT NULL DEFAULT 0,
    fversion INT NOT NULL DEFAULT 0,
    PRIMARY KEY (fid),
    UNIQUE KEY uk_matrix_base_partner_tax (
        ftenant_id, fbusiness_partner_id, fdelete_flag
    )
) COMMENT='BusinessPartner税务档案';

CREATE TABLE IF NOT EXISTS matrix_base_business_partner_settlement (
    fid BIGINT NOT NULL,
    ftenant_id VARCHAR(64) NOT NULL,
    fbusiness_partner_id BIGINT NOT NULL,
    fbusiness_partner_role_id BIGINT NOT NULL,
    fsettlement_type VARCHAR(32) NULL,
    fpayment_term_code VARCHAR(64) NULL,
    fsettlement_currency_id BIGINT NULL,
    fsettlement_method VARCHAR(64) NULL,
    fpayment_method VARCHAR(64) NULL,
    fcredit_days INT NULL,
    fadvance_required TINYINT NOT NULL DEFAULT 0,
    fstatus VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
    fcreate_by BIGINT NULL,
    fcreate_time DATETIME NOT NULL,
    fmodify_by BIGINT NULL,
    fmodify_time DATETIME NULL,
    fdelete_flag TINYINT NOT NULL DEFAULT 0,
    fversion INT NOT NULL DEFAULT 0,
    PRIMARY KEY (fid),
    UNIQUE KEY uk_matrix_base_partner_settlement (
        ftenant_id, fbusiness_partner_role_id, fdelete_flag
    ),
    KEY idx_matrix_base_partner_settlement_partner (
        ftenant_id, fbusiness_partner_id
    )
) COMMENT='BusinessPartner角色结算档案';
