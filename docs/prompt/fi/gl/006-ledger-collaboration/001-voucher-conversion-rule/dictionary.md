# 凭证折算规则数据字典提示词

## 目标
根据 `docs/bus/fi/gl/006-ledger-collaboration/001-voucher-conversion-rule/` 下业务文档，输出凭证折算规则场景的数据字典、统计口径、覆盖率口径、告警口径与跳转口径，作为接口、前后端、测试的统一语义基础。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`

同时结合当前已确认实现：
- 后端服务：`BizfiFiLedgerCollaborationServiceImpl.voucherRules`
- 数据来源：内置 `RuleSpec` 与 AR/AP 单据实际生成情况
- 前端页面：`VoucherRuleView.vue`
- 接口：`GET /ledger-collaboration/voucher-rules`

## 输出要求
1. 输出查询条件字段字典
2. 输出汇总卡片字段字典
3. 输出规则清单字段字典
4. 输出规则数、已审核单据、已生成凭证、待生成、覆盖率、最近单据日期等统计口径
5. 输出 warnings 与跳转到应收/应付业务单据页面口径
6. 对 BUS 未明确项显式标记“待确认”
