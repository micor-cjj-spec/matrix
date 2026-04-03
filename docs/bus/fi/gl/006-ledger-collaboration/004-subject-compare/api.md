# 接口说明

## 查询接口
- `GET /ledger-collaboration/subject-balance-compare`

## 参数
- `startDate`
- `endDate`
- `accountCode`
- `diffOnly`

## 返回结构
返回 `Map<String,Object>`：
- `rows`
- `warnings`
- `accountCount`
- `diffAccountCount`
- `voucherDebitTotal`
- `glDebitTotal`
- `voucherCreditTotal`
- `glCreditTotal`

## 前端关联
- 前端 API：`ledgerCollaborationApi.fetchSubjectBalanceCompare`
- 前端页面：`SubjectCompareView.vue`
