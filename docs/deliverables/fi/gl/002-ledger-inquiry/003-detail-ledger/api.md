# 明细分类账接口说明

## 1. 查询接口
- `GET /ledger/detail-ledger`

## 2. 查询参数
- `startDate`
- `endDate`
- `accountCode`

## 3. 返回结构
返回 `LedgerQueryResultVO`：
- `records`：`LedgerBookRowVO[]`
- `warnings`
- `summary`

## 4. 前端关联
- 前端 API：`ledgerApi.fetchDetailLedger`
- 前端页面：`DetailLedgerView.vue`

## 5. 说明
- 当前场景为查询类账表，不包含写操作接口
- 返回重点在明细流水、汇总信息和 warnings
