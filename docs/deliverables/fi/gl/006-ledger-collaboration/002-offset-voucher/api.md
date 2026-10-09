# 对冲凭证接口说明

## 1. 查询接口
- `GET /ledger-collaboration/offset-vouchers`

## 2. 查询参数
- `startDate`
- `endDate`
- `matchStatus`
- `keyword`

## 3. 返回结构
返回 `Map<String,Object>`：
- `rows`
- `warnings`
- `pairCount`
- `orphanCount`
- `originalAmount`
- `reverseAmount`

## 4. 前端关联
- 前端 API：`ledgerCollaborationApi.fetchOffsetVouchers`
- 前端页面：`OffsetVoucherView.vue`

## 5. 说明
- 当前场景为查询类链路追踪页，不包含写操作接口
- 返回重点在配对结果、warnings 与汇总统计字段
