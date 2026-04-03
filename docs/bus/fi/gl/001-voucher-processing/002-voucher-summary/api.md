# 接口说明

## 汇总接口
- `GET /voucher/summary`

## 查询参数
- `startDate`：开始日期，格式 `yyyy-MM-dd`
- `endDate`：结束日期，格式 `yyyy-MM-dd`
- `status`：凭证状态
- `summaryKeyword`：摘要关键字

## 返回结构
返回 `VoucherSummaryResultVO`，核心字段：
- `totalCount`
- `totalAmount`
- `draftCount`
- `submittedCount`
- `auditedCount`
- `postedCount`
- `rejectedCount`
- `reversedCount`
- `postedAmount`
- `warnings`
- `rows`

## 前端关联
- 前端 API：`voucherApi.fetchSummary`
- 前端页面：`VoucherSummaryView.vue`
- 反查跳转：点击表格“查看凭证”后跳转到 `/ledger/voucher`
