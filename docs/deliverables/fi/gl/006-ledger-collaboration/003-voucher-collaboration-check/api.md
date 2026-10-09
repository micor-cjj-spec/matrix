# 凭证协同检查接口说明

## 1. 查询接口
- `GET /ledger-collaboration/voucher-check`

## 2. 查询参数
- `startDate`
- `endDate`
- `issueCode`
- `severity`
- `status`
- `onlyIssue`

## 3. 返回结构
返回 `Map<String,Object>`：
- `rows`
- `warnings`
- `voucherCount`
- `issueCount`
- `issueVoucherCount`
- `highCount`
- `healthyCount`

## 4. 前端关联
- 前端 API：`ledgerCollaborationApi.fetchVoucherCheck`
- 前端页面：`VoucherCollaborationCheckView.vue`

## 5. 说明
- 当前场景为查询类检查页，不包含写操作接口
- 返回重点在协同检查结果、warnings 与汇总统计字段
