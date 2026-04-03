# 接口说明

## 查询接口
- `GET /ledger/daily-report`

## 查询参数
- `startDate`
- `endDate`
- `accountCode`

## 返回结构
返回 `LedgerQueryResultVO`：
- `records`：`LedgerDailyRowVO[]`
- `warnings`
- `summary`

## 前端关联
- 前端 API：`ledgerApi.fetchDailyReport`
- 前端页面：`DailyReportView.vue`
