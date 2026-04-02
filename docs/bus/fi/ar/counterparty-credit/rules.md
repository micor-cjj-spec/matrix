# 业务规则

## 基础规则
- `fcounterparty` 不能为空。fileciteturn167file0L215-L217
- `fdocTypeRoot` 仅支持 `AR / AP`。fileciteturn167file0L421-L426
- `fcreditLimit` 必须大于 0。fileciteturn167file0L218-L220

## 默认值规则
- `foverdueDaysThreshold` 为空或小于 0 时默认 30。fileciteturn167file0L221-L224
- `fenabled` 为空时默认 1。fileciteturn167file0L224-L226
- `fblockOnOverLimit` 和 `fblockOnOverdue` 为空时默认 0。fileciteturn167file0L227-L229

## 唯一性规则
- 配置按 `counterparty + docTypeRoot` 唯一保存；若已存在则更新，不新建重复记录。fileciteturn167file0L231-L254

## 联动规则
- 提交和审核往来单前都会检查信用配置。fileciteturn167file0L349-L360
- 当 `overLimit && blockOnOverLimit` 或 `overdue && blockOnOverdue` 成立时，业务会被硬拦截。fileciteturn167file0L390-L413
- 预警统计依赖按往来方聚合的未结清金额与最大逾期天数。fileciteturn167file0L154-L213
