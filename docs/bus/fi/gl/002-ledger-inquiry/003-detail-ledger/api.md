# 接口说明

## 查询接口
- `GET /ledger/detail-ledger`

## 查询参数
- `startDate`
- `endDate`
- `accountCode`

## 返回结构
返回 `LedgerQueryResultVO`：
- `records`：`LedgerBookRowVO[]`
- `warnings`
- `summary`

## 前端关联
- 前端 API：`ledgerApi.fetchDetailLedger`
- 前端页面：`DetailLedgerView.vue`
