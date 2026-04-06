# 现金流量表接口说明

## 1. 查询接口
- `GET /cash-flow`

## 2. 查询参数
- `orgId`
- `period`
- `currency`
- `templateId`
- `showZero`

## 3. 返回结构
返回 `ReportQueryResultVO`：
- `rows`
- `checks`
- `warnings`
- `templateId`
- `templateName`

## 4. 前端关联
- 前端 API：`financialReportApi.fetchCashFlow`
- 前端页面：`CashFlowView.vue`
- 跳转按钮：现金流量查询、补充资料

## 5. 说明
- 当前场景为查询类报表，不包含写操作接口
- 返回重点在报表行、checks、warnings 与模板信息
