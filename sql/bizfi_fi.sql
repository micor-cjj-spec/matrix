CREATE TABLE bizfi_fi_account (
                                  fid              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
                                  fcode            VARCHAR(64)  NOT NULL COMMENT '科目编码',
                                  fname            VARCHAR(200) NOT NULL COMMENT '科目名称',
                                  forg             BIGINT       NOT NULL COMMENT '组织ID',
                                  flong_name       VARCHAR(255) COMMENT '全称',
                                  ftype            VARCHAR(50)  NOT NULL COMMENT '科目类型',
                                  fparent          BIGINT       COMMENT '上级科目ID',
                                  fpltype          VARCHAR(50)  COMMENT '利润表类型',
                                  fdirection       VARCHAR(10)  NOT NULL COMMENT '方向',
                                  fis_detail       TINYINT(1)   DEFAULT 0 COMMENT '是否明细科目',
                                  freport_item     BIGINT       COMMENT '关联报表项目ID',
                                  flevel1          BIGINT       COMMENT '一级科目ID',
                                  fentry_control   VARCHAR(20)  COMMENT '分录控制',
                                  fcontrol_level   VARCHAR(20)  COMMENT '控制级别',
                                  fallow_child     TINYINT(1)   DEFAULT 0 COMMENT '是否允许子科目',
                                  fmanual_entry    TINYINT(1)   DEFAULT 1 COMMENT '是否手工录入',
                                  fcash            TINYINT(1)   DEFAULT 0 COMMENT '是否现金科目',
                                  fbank            TINYINT(1)   DEFAULT 0 COMMENT '是否银行科目',
                                  fequivalent      TINYINT(1)   DEFAULT 0 COMMENT '是否等价物科目',
                                  fis_entry        TINYINT(1)   DEFAULT 0 COMMENT '是否可录凭证分录',
                                  fnotice          TINYINT(1)   DEFAULT 0 COMMENT '是否通知',
                                  fexchange        TINYINT(1)   DEFAULT 0 COMMENT '是否外币科目',
                                  fqty_accounting  TINYINT(1)   DEFAULT 0 COMMENT '是否数量核算',
                                  fcreatetime      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                  fupdatetime      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                  INDEX idx_fcode (fcode),
                                  INDEX idx_forg (forg),
                                  INDEX idx_fparent (fparent)
) COMMENT='会计科目表';
INSERT INTO bizfi_fi_account
(fcode, fname, forg, flong_name, ftype, fparent, fpltype, fdirection, fis_detail, freport_item, flevel1, fentry_control, fcontrol_level, fallow_child, fmanual_entry, fcash, fbank, fequivalent, fis_entry, fnotice, fexchange, fqty_accounting)
VALUES
    ('1001', '库存现金', 1, '公司本部-库存现金', '资产', NULL, '资产类', '借', 1, NULL, NULL, NULL, NULL, 0, 1, 1, 0, 0, 1, 0, 0, 0);

INSERT INTO bizfi_fi_account
(fcode, fname, forg, flong_name, ftype, fparent, fpltype, fdirection, fis_detail, freport_item, flevel1, fentry_control, fcontrol_level, fallow_child, fmanual_entry, fcash, fbank, fequivalent, fis_entry, fnotice, fexchange, fqty_accounting)
VALUES
    ('1002', '银行存款', 1, '公司本部-银行存款', '资产', NULL, '资产类', '借', 1, NULL, NULL, NULL, NULL, 0, 1, 0, 1, 0, 1, 0, 1, 0);

INSERT INTO bizfi_fi_account
(fcode, fname, forg, flong_name, ftype, fparent, fpltype, fdirection, fis_detail, freport_item, flevel1, fentry_control, fcontrol_level, fallow_child, fmanual_entry, fcash, fbank, fequivalent, fis_entry, fnotice, fexchange, fqty_accounting)
VALUES
    ('1011', '应收账款', 1, '公司本部-应收账款', '资产', NULL, '资产类', '借', 1, NULL, NULL, NULL, NULL, 0, 0,1, 0, 0, 1, 0, 0, 0);

INSERT INTO bizfi_fi_account
(fcode, fname, forg, flong_name, ftype, fparent, fpltype, fdirection, fis_detail, freport_item, flevel1, fentry_control, fcontrol_level, fallow_child, fmanual_entry, fcash, fbank, fequivalent, fis_entry, fnotice, fexchange, fqty_accounting)
VALUES
    ('2001', '短期借款', 1, '公司本部-短期借款', '负债', NULL, '负债类', '贷', 1, NULL, NULL, NULL, NULL, 0, 1, 0, 0, 0, 1, 0, 0, 0);

INSERT INTO bizfi_fi_account
(fcode, fname, forg, flong_name, ftype, fparent, fpltype, fdirection, fis_detail, freport_item, flevel1, fentry_control, fcontrol_level, fallow_child, fmanual_entry, fcash, fbank, fequivalent, fis_entry, fnotice, fexchange, fqty_accounting)
VALUES
    ('3001', '主营业务收入', 1, '公司本部-主营业务收入', '收入', NULL, '收入类', '贷', 1, NULL, NULL, NULL, NULL, 0, 1, 0, 0, 0, 1, 0, 0, 0);
