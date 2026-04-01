create table bizfi_base.bizfi_base_user
(
    fid              bigint       not null comment '主键ID'
        primary key,
    ftid             bigint       null comment '团队ID',
    fnumber          varchar(36)  null comment '工号',
    fphone           varchar(36)  null comment '手机号码',
    femail           varchar(100) null comment '电子邮箱',
    fstatus          varchar(15)  null comment '数据状态（启用/停用/草稿等）',
    fgender          varchar(15)  null comment '性别（男/女/其他）',
    fbirthday        timestamp    null comment '出生日期',
    fidcard          varchar(20)  null comment '身份证号码',
    favatar          varchar(300) null comment '用户头像（头像URL）',
    fnickname        varchar(300) null comment '昵称',
    ffullpinyin      varchar(100) null comment '姓名全拼',
    fsimplepinyin    varchar(50)  null comment '姓名拼音缩写',
    fdptid           bigint       null comment '所属部门ID',
    fpositionid      bigint       null comment '岗位ID',
    fsortcode        varchar(10)  null comment '排序编号',
    fbillssatusfield varchar(50)  null comment '单据状态字段',
    fhiredate        timestamp    null comment '入职日期',
    fenable          char         null comment '是否启用（Y/N）',
    fcreatorid       bigint       null comment '创建人ID',
    fcreatetime      timestamp    null comment '创建时间',
    fmodifierid      bigint       null comment '最后修改人ID',
    fmodifytime      timestamp    null comment '最后修改时间',
    fdisablerid      bigint       null comment '停用人ID',
    fdisabledate     timestamp    null comment '停用时间',
    fsource          varchar(10)  null comment '数据来源（导入/注册/同步）',
    fmaintain        varchar(10)  null comment '维护方式（手动/接口）',
    fstartdate       timestamp    null comment '有效开始时间',
    fenddate         timestamp    null comment '有效结束时间',
    fmasterid        bigint       null comment '主用户ID（用于主从账号绑定）',
    fheadsculpture   varchar(300) null comment '个性头像或自定义形象',
    ftruename        varchar(255) null comment '真实姓名',
    fcountryid       bigint       null comment '所属国家ID'
)
    comment '基础用户信息表';

INSERT INTO bizfi_base.bizfi_base_user
(fid, ftid, fnumber, fphone, femail, fstatus, fgender, fbirthday, fidcard, favatar, fnickname, ffullpinyin, fsimplepinyin, fdptid, fpositionid, fsortcode, fbillssatusfield, fhiredate, fenable, fcreatorid, fcreatetime, fmodifierid, fmodifytime, fdisablerid, fdisabledate, fsource, fmaintain, fstartdate, fenddate, fmasterid, fheadsculpture, ftruename, fcountryid)
VALUES
(806253418729055924, 0, 'admin', '19106026235', '2060254584@qq.com', '启用', '男', NULL, NULL, NULL, '系统管理员', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'Y', 0, '2025-06-09 12:10:04', NULL, NULL, NULL, NULL, 'manual', NULL, NULL, NULL, NULL, NULL, '李京泷', NULL);
