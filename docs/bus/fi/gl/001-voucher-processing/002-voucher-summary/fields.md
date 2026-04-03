# 字段设计

## 查询参数
| 参数 | 含义 | 说明 |
|---|---|---|
| startDate | 开始日期 | `yyyy-MM-dd` |
| endDate | 结束日期 | `yyyy-MM-dd` |
| status | 状态 | 可选：`DRAFT / SUBMITTED / AUDITED / POSTED / REJECTED / REVERSED` |
| summaryKeyword | 摘要关键字 | 仅匹配凭证头摘要 |

## 汇总头字段（VoucherSummaryResultVO）
| 字段 | 含义 |
|---|---|
| startDate | 查询开始日期 |
| endDate | 查询结束日期 |
| status | 查询状态 |
| summaryKeyword | 查询摘要关键字 |
| totalCount | 凭证总数 |
| totalAmount | 合计金额 |
| draftCount | 草稿数 |
| submittedCount | 已提交数 |
| auditedCount | 已审核数 |
| postedCount | 已过账数 |
| rejectedCount | 已驳回数 |
| reversedCount | 已冲销数 |
| postedAmount | 已过账金额 |
| warnings | 提示信息 |
| rows | 按日期聚合的明细行 |

## 表格行字段（VoucherSummaryRowVO）
| 字段 | 含义 |
|---|---|
| bizDate | 业务日期 |
| voucherCount | 当日凭证数 |
| totalAmount | 当日合计金额 |
| draftCount | 当日草稿数 |
| submittedCount | 当日已提交数 |
| auditedCount | 当日已审核数 |
| postedCount | 当日已过账数 |
| rejectedCount | 当日已驳回数 |
| reversedCount | 当日已冲销数 |
| postedAmount | 当日已过账金额 |
