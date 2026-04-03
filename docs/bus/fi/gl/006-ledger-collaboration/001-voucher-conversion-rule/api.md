# 接口说明

## 查询接口
- `GET /ledger-collaboration/voucher-rules`

## 参数
- `docTypeRoot`
- `keyword`

## 返回结构
返回 `Map<String,Object>`：
- `rows`
- `warnings`
- `ruleCount`
- `auditedCount`
- `generatedCount`
- `pendingCount`

## 前端关联
- 前端 API：`ledgerCollaborationApi.fetchVoucherRules`
- 前端页面：`VoucherRuleView.vue`
