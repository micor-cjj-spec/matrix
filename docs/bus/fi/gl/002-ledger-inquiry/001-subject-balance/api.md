# 接口说明

## 查询接口
- `GET /ledger/subject-balance`

## 查询参数
- `startDate`
- `endDate`
- `accountCode`

## 返回结构
返回 `LedgerQueryResultVO`：
- `records`：`LedgerBalanceRowVO[]`
- `warnings`：提示信息
- `summary`：汇总信息

## 前端关联
- 前端 API：`ledgerApi.fetchSubjectBalance`
- 前端页面：`SubjectBalanceView.vue`
