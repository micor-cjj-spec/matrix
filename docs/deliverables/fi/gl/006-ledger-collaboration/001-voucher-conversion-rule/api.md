# 凭证折算规则接口说明

## 1. 查询接口
- `GET /ledger-collaboration/voucher-rules`

## 2. 查询参数
- `docTypeRoot`
- `keyword`

## 3. 返回结构
返回 `Map<String,Object>`：
- `rows`
- `warnings`
- `ruleCount`
- `auditedCount`
- `generatedCount`
- `pendingCount`

## 4. 前端关联
- 前端 API：`ledgerCollaborationApi.fetchVoucherRules`
- 前端页面：`VoucherRuleView.vue`

## 5. 说明
- 当前场景为查询类规则展示页，不包含写操作接口
- 返回重点在规则清单、warnings 与汇总统计字段
