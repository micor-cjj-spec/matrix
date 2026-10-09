# 现金流量查询接口说明

## 1. 查询接口
- `GET /cash-flow/query`

## 2. 查询参数
- `orgId`
- `period`
- `currency`
- `cashflowItemCode`
- `categoryCode`
- `sourceType`
- `accountCode`
- `keyword`

## 3. 返回结构
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

## 4. 前端关联
- 前端 API：`financialReportApi.fetchCashFlowQuery`
- 前端页面：`CashFlowQueryView.vue`
- 点击“查看凭证”跳转凭证页

## 5. 说明
- 当前场景为查询类追踪页，不包含写操作接口
- 返回重点在追踪记录、warnings 和识别来源统计
