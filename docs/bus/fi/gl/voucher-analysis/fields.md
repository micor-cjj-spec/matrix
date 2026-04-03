# 字段设计

## 汇总分析输入字段

| 字段 | 含义 | 类型 | 是否必填 | 备注 |
|---|---|---|---|---|
| startDate | 开始日期 | string | 否 | yyyy-MM-dd |
| endDate | 结束日期 | string | 否 | yyyy-MM-dd |
| status | 凭证状态 | string | 否 | 统一转大写 |
| summaryKeyword | 摘要关键字 | string | 否 | 模糊匹配凭证头摘要 |

## 汇总分析结果字段

| 字段 | 含义 | 类型 | 备注 |
|---|---|---|---|
| totalCount | 总笔数 | int | |
| totalAmount | 总金额 | decimal | |
| postedAmount | 已过账金额 | decimal | |
| draftCount | 草稿数 | int | |
| submittedCount | 已提交数 | int | |
| auditedCount | 已审核数 | int | |
| postedCount | 已过账数 | int | |
| rejectedCount | 已驳回数 | int | |
| reversedCount | 已冲销数 | int | |
| rows | 分日期汇总行 | array | |
| warnings | 警告信息 | array | |

## 结转清单输入字段

| 字段 | 含义 | 类型 | 是否必填 | 备注 |
|---|---|---|---|---|
| forg | 组织ID | Long | 否 | |
| period | 期间 | string | 否 | yyyy-MM |

## 结转清单结果字段

| 字段 | 含义 | 类型 | 备注 |
|---|---|---|---|
| baseCurrency | 本位币 | string | |
| currentPeriod | 当前期间 | string | |
| defaultVoucherType | 默认凭证字 | string | |
| tasks | 检查任务列表 | array | |
| relatedVouchers | 相关结转凭证 | array | |
| warnings | 警告信息 | array | |
