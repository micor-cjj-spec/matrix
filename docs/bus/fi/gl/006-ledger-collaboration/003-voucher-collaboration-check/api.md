# 接口说明

## 查询接口
- `GET /ledger-collaboration/voucher-check`

## 参数
- `startDate`
- `endDate`
- `issueCode`
- `severity`
- `status`
- `onlyIssue`

## 返回结构
返回 `Map<String,Object>`：
- `rows`
- `warnings`
- `voucherCount`
- `issueCount`
- `issueVoucherCount`
- `highCount`
- `healthyCount`

## 前端关联
- 前端 API：`ledgerCollaborationApi.fetchVoucherCheck`
- 前端页面：`VoucherCollaborationCheckView.vue`
