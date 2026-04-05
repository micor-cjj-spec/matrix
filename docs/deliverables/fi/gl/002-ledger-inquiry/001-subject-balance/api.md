# 科目余额表接口说明

## 1. 查询接口
- `GET /ledger/subject-balance`

## 2. 查询参数
- `startDate`
- `endDate`
- `accountCode`

## 3. 返回结构
返回 `LedgerQueryResultVO`：
- `records`：`LedgerBalanceRowVO[]`
- `warnings`：提示信息
- `summary`：汇总信息

## 4. 前端关联
- 前端 API：`ledgerApi.fetchSubjectBalance`
- 前端页面：`SubjectBalanceView.vue`
- 路由 query 支持带入 `startDate / endDate / accountCode`

## 5. 说明
- 当前场景为查询类账表，不包含写操作接口
- 返回重点在余额记录、汇总信息和 warnings
