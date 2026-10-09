# 凭证折算规则SQL提示词

## 目标
根据 `docs/bus/fi/gl/006-ledger-collaboration/001-voucher-conversion-rule/` 下业务文档，以及当前已确认实现，输出凭证折算规则场景的 SQL 与查询设计提示词。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `rules.md`
- `api.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/001-voucher-conversion-rule/dictionary.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/001-voucher-conversion-rule/backend.md`

同时结合当前已确认实现：
- 基于内置 `RuleSpec` 与 AR/AP 单据实际生成情况生成结果
- 需要按 docTypeRoot、keyword 过滤

## 输出要求
1. 输出 `rows`、`warnings` 与统计字段所需查询口径
2. 输出规则配置、实际单据、凭证生成情况关联口径
3. 输出覆盖率、待生成量、最近单据日期相关数据来源建议
4. 输出索引与性能优化建议
5. 对 BUS 未明确项显式标记“待确认”
