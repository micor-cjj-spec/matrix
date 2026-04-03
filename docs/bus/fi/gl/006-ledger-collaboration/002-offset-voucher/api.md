# 接口说明

## 查询接口
- `GET /ledger-collaboration/offset-vouchers`

## 参数
- `startDate`
- `endDate`
- `matchStatus`
- `keyword`

## 返回结构
返回 `Map<String,Object>`：
- `rows`
- `warnings`
- `pairCount`
- `orphanCount`
- `originalAmount`
- `reverseAmount`

## 前端关联
- 前端 API：`ledgerCollaborationApi.fetchOffsetVouchers`
- 前端页面：`OffsetVoucherView.vue`
