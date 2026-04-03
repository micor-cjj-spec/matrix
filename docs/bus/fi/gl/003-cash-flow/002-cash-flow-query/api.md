# 接口说明

## 查询接口
- `GET /cash-flow/query`

## 查询参数
- `orgId`
- `period`
- `currency`
- `cashflowItemCode`
- `categoryCode`
- `sourceType`
- `accountCode`
- `keyword`

## 返回结构
返回 `CashFlowTraceResultVO`：
- `records`
- `warnings`
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

## 前端关联
- 前端 API：`financialReportApi.fetchCashFlowQuery`
- 前端页面：`CashFlowQueryView.vue`
