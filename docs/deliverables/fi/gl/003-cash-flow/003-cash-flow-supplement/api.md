# 现金流量补充资料接口说明

## 1. 查询接口
- `GET /cash-flow/supplement`

## 2. 查询参数
- `orgId`
- `period`
- `currency`

## 3. 返回结构
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

## 4. 前端关联
- 前端 API：`financialReportApi.fetchCashFlowSupplement`
- 前端页面：`CashFlowSupplementView.vue`
- 点击“查看凭证”跳转凭证页

## 5. 说明
- 当前场景为查询类补充资料页，不包含写操作接口
- 返回重点在任务清单、分类分布、待补录/复核凭证、warnings 与统计字段
