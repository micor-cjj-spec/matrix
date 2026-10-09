# 凭证汇总表接口说明

## 1. 汇总接口
- `GET /voucher/summary`

## 2. 查询参数
- `startDate`：开始日期，格式 `yyyy-MM-dd`
- `endDate`：结束日期，格式 `yyyy-MM-dd`
- `status`：凭证状态
- `summaryKeyword`：摘要关键字

## 3. 返回结构
返回 `VoucherSummaryResultVO`，核心字段包括：
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

## 4. 前端关联
- 前端 API：`voucherApi.fetchSummary`
- 前端页面：`VoucherSummaryView.vue`
- 反查跳转：点击“查看凭证”后跳转 `/ledger/voucher`

## 5. 说明
- 当前场景为只读统计分析，不包含新增、编辑、删除接口
- 当前返回结构重点在聚合结果与 warnings，不应扩展为制单接口
