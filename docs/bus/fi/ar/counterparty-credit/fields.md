# 字段设计

## 主表字段

| 字段 | 含义 | 类型 | 是否必填 | 备注 |
|---|---|---|---|---|
| fid | 主键 | Long | 是 | |
| fcounterparty | 往来方 | string | 是 | |
| fdocTypeRoot | 根业务类型 | string | 是 | 仅支持 AR / APfileciteturn165file0L14-L18 |
| fcreditLimit | 信用额度 | decimal | 是 | 必须大于 0fileciteturn167file0L215-L220 |
| foverdueDaysThreshold | 逾期天数阈值 | int | 否 | 为空或非法时默认 30fileciteturn167file0L221-L224 |
| fenabled | 启用标识 | int | 否 | 默认 1fileciteturn167file0L224-L229 |
| fblockOnOverLimit | 超额度硬拦截 | int | 否 | 1 表示命中时阻断提交/审核fileciteturn165file0L18-L21 |
| fblockOnOverdue | 超逾期硬拦截 | int | 否 | 1 表示命中时阻断提交/审核fileciteturn165file0L19-L21 |
| fremark | 备注 | string | 否 | |
| fupdatedBy | 更新人 | string | 否 | |
| fupdatedTime | 更新时间 | datetime | 否 | |

## 明细字段

当前代码模型为单表配置，无独立明细表。fileciteturn165file0L1-L22
