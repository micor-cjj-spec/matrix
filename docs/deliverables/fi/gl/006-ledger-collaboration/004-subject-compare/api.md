# 科目余额对照接口说明

## 1. 查询接口
- `GET /ledger-collaboration/subject-balance-compare`

## 2. 查询参数
- `startDate`
- `endDate`
- `accountCode`
- `diffOnly`

## 3. 返回结构
返回 `Map<String,Object>`：
- `rows`
- `warnings`
- `accountCount`
- `diffAccountCount`
- `voucherDebitTotal`
- `glDebitTotal`
- `voucherCreditTotal`
- `glCreditTotal`

## 4. 前端关联
- 前端 API：`ledgerCollaborationApi.fetchSubjectBalanceCompare`
- 前端页面：`SubjectCompareView.vue`

## 5. 说明
- 当前场景为查询类对照页，不包含写操作接口
- 返回重点在科目对照结果、warnings 与汇总统计字段
