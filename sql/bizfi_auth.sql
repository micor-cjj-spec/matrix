create table bizfi_auth.bizfi_auth_login
(
    fid         bigint                             not null comment '主键ID'
        primary key,
    fuserid     bigint                             not null comment '关联用户FID',
    fpassword   varchar(64)                        not null comment '登录密码（MD5加密）',
    fcreatetime datetime default CURRENT_TIMESTAMP null comment '创建时间',
    fmodifytime datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '修改时间',
    fcreatorid  bigint                             null comment '创建人ID',
    fmodifierid bigint                             null comment '修改人ID'
)
    comment '用户登录认证表';

INSERT INTO bizfi_auth.bizfi_auth_login
(fid, fuserid, fpassword, fcreatetime, fmodifytime, fcreatorid, fmodifierid)
VALUES
(806253418729055925, 806253418729055924, 'e10adc3949ba59abbe56e057f20f883e', '2025-06-09 12:10:41', '2025-06-09 12:10:41', 1, 1);
