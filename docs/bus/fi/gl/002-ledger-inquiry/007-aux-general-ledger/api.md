# 接口说明

## 查询接口
- `GET /ledger/aux-general-ledger`

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
- 前端 API：`ledgerApi.fetchAuxGeneralLedger`
- 前端页面：`AuxGeneralLedgerView.vue`
