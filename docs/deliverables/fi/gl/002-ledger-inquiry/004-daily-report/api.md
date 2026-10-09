# 日报表接口说明

## 1. 查询接口
- `GET /ledger/daily-report`

## 2. 查询参数
- `startDate`
- `endDate`
- `accountCode`

## 3. 返回结构
返回 `LedgerQueryResultVO`：
- `records`：`LedgerDailyRowVO[]`
- `warnings`
- `summary`

## 4. 前端关联
- 前端 API：`ledgerApi.fetchDailyReport`
- 前端页面：`DailyReportView.vue`

## 5. 说明
- 当前场景为查询类日报，不包含写操作接口
- 返回重点在日级统计记录、汇总信息和 warnings
