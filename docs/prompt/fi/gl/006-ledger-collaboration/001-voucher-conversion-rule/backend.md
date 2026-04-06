# 凭证折算规则后端提示词

## 目标
结合 `docs/bus/fi/gl/006-ledger-collaboration/001-voucher-conversion-rule/` 下业务文档，以及当前后端已确认实现，输出凭证折算规则场景的后端实现建议与补齐提示词。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `api.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/001-voucher-conversion-rule/dictionary.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/001-voucher-conversion-rule/api.md`

同时结合当前已确认实现：
- 查询服务：`BizfiFiLedgerCollaborationServiceImpl.voucherRules`
- 基于内置 `RuleSpec` 与 AR/AP 单据实际生成情况生成结果
- 返回 `rows`、`warnings` 与统计字段

## 输出要求
1. 说明当前后端规则展示与覆盖率分析能力与待补齐项
2. 输出规则映射、借贷科目、覆盖率、待生成量、最近单据日期等计算逻辑建议
3. 说明 docTypeRoot、keyword 参数处理逻辑
4. 说明性能、空结果与异常结果处理建议
5. 对 BUS 未明确项显式标记“待确认”
