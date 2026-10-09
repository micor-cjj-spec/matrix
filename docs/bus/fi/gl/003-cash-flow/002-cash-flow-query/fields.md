# 字段设计

## 查询参数
| 参数 | 含义 | 说明 |
|---|---|---|
| orgId | 业务单元 | 可空 |
| period | 期间 | 必须为 `yyyy-MM` |
| currency | 币种 | 默认 `CNY` |
| cashflowItemCode | 现金流项目编码 | 可空 |
| categoryCode | 活动分类 | `CF_OPERATING / CF_INVESTING / CF_FINANCING` 等 |
| sourceType | 识别方式 | `DIRECT / HEURISTIC / UNKNOWN_ITEM / MIXED_ITEM / CASH_TRANSFER` |
| accountCode | 科目编码关键字 | 同时匹配现金科目和对方科目 |
| keyword | 关键字 | 匹配凭证号、摘要、现金流项目 |

## 返回结构
返回 `CashFlowTraceResultVO`，核心字段包括：
- `postedVoucherCount`
- `cashVoucherCount`
- `directCount`
- `heuristicCount`
- `unknownCount`
- `mixedCount`
- `transferCount`
- `cashInAmount`
- `cashOutAmount`
- `netAmount`
- `records`
- `warnings`

## 记录行（CashFlowTraceRowVO）
| 字段 | 含义 |
|---|---|
| voucherId | 凭证ID |
| voucherNumber | 凭证号 |
| voucherDate | 凭证日期 |
| summary | 摘要 |
| cashAccountCodes | 现金类科目 |
| counterpartyAccountCodes | 对方科目 |
| cashflowItemCode | 现金流项目编码 |
| cashflowItemName | 现金流项目名称 |
| categoryCode | 活动分类编码 |
| categoryName | 活动分类名称 |
| sourceType | 识别方式 |
| cashInAmount | 现金流入 |
| cashOutAmount | 现金流出 |
| netAmount | 现金净额 |
| reason | 分类说明 |
