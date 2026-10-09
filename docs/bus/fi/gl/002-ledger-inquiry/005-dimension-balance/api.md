# 接口说明

## 查询接口
- `GET /ledger/dimension-balance`

## 查询参数
- `startDate`
- `endDate`
- `accountCode`
- `dimensionCode`

## 返回结构
返回 `LedgerQueryResultVO`：
- `records`：`LedgerDimensionBalanceRowVO[]`
- `warnings`
- `summary`

## 前端关联
- 前端 API：`ledgerApi.fetchDimensionBalance`
- 前端页面：`DimensionBalanceView.vue`
