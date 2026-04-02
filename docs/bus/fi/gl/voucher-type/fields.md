# 字段设计

## 主表字段

| 字段 | 含义 | 类型 | 是否必填 | 备注 |
|---|---|---|---|---|
| fid | 主键 | Long | 是 | |
| forg | 组织ID | Long | 是 | |
| fcode | 凭证字编码 | string | 是 | 同组织唯一，保存时转大写fileciteturn239file0L108-L117 |
| fname | 凭证字名称 | string | 是 | |
| fprefix | 编号前缀 | string | 否 | 为空时可不填 |
| fsort | 排序 | int | 否 | 默认 0fileciteturn239file0L117-L118 |
| fstatus | 状态 | string | 是 | ENABLED / DISABLEDfileciteturn239file0L119-L129 |
| fremark | 备注 | string | 否 | |
| fcreatetime | 创建时间 | datetime | 否 | |
| fupdatetime | 更新时间 | datetime | 否 | |

## 明细字段

当前代码模型为单表配置，无独立明细表。fileciteturn226file0L1-L29
