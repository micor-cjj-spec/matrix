# 接口说明

## 查询接口
- `GET /cash-flow/supplement`

## 查询参数
- `orgId`
- `period`
- `currency`

## 返回结构
返回 `CashFlowSupplementResultVO`：
- `tasks`
- `categories`
- `pendingVouchers`
- `warnings`
- `postedVoucherCount`
- `cashVoucherCount`
- `cashAccountCount`
- `cashflowItemCount`
- `directCount`
- `heuristicCount`
- `unknownCount`
- `mixedCount`
- `transferCount`
- `cashInAmount`
- `cashOutAmount`
- `netAmount`

## 前端关联
- 前端 API：`financialReportApi.fetchCashFlowSupplement`
- 前端页面：`CashFlowSupplementView.vue`
