# 现金流量查询后端提示词

## 目标
结合 `docs/bus/fi/gl/003-cash-flow/002-cash-flow-query/` 下业务文档，以及当前后端已确认实现，输出现金流量查询场景的后端实现建议与补齐提示词。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `api.md`
- `docs/prompt/fi/gl/003-cash-flow/002-cash-flow-query/dictionary.md`
- `docs/prompt/fi/gl/003-cash-flow/002-cash-flow-query/api.md`

同时结合当前已确认实现：
- 查询服务：`BizfiFiCashFlowServiceImpl.trace`
- 返回 `records`、`warnings` 和各类统计计数/金额

## 输出要求
1. 说明当前后端追踪能力与待补齐项
2. 输出直接标记、规则推断、未知编码、多编码复核、现金划转等识别逻辑建议
3. 说明 orgId、period、currency、cashflowItemCode、categoryCode、sourceType、accountCode、keyword 过滤逻辑
4. 说明统计汇总、性能、空结果与异常结果处理建议
5. 对 BUS 未明确项显式标记“待确认”
