# 接口说明

## 查询接口
- `GET /ledger/aux-detail-ledger`

## 查询参数
- `startDate`
- `endDate`
- `accountCode`
- `dimensionCode`

## 返回结构
返回 `LedgerQueryResultVO`：
- `records`：`LedgerBookRowVO[]`
- `warnings`
- `summary`

## 前端关联
- 前端 API：`ledgerApi.fetchAuxDetailLedger`
- 前端页面：`AuxDetailLedgerView.vue`
