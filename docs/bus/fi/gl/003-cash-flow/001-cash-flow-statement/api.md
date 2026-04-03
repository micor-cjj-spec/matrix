# 接口说明

## 查询接口
- `GET /cash-flow`

## 查询参数
- `orgId`
- `period`
- `currency`
- `templateId`
- `showZero`

## 返回结构
返回 `ReportQueryResultVO`：
- `rows`
- `checks`
- `warnings`
- `templateId`
- `templateName`

## 前端关联
- 前端 API：`financialReportApi.fetchCashFlow`
- 前端页面：`CashFlowView.vue`
