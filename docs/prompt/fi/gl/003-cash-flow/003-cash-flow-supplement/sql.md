# 现金流量补充资料SQL提示词

## 目标
根据 `docs/bus/fi/gl/003-cash-flow/003-cash-flow-supplement/` 下业务文档，以及当前已确认实现，输出现金流量补充资料场景的 SQL 与查询设计提示词。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `rules.md`
- `api.md`
- `docs/prompt/fi/gl/003-cash-flow/003-cash-flow-supplement/dictionary.md`
- `docs/prompt/fi/gl/003-cash-flow/003-cash-flow-supplement/backend.md`

同时结合当前已确认实现：
- 基于现金相关凭证分析结果生成任务清单、分类分布与待复核凭证
- 需要按 orgId、period、currency 聚合统计

## 输出要求
1. 输出 `tasks`、`categories`、`pendingVouchers`、`warnings` 所需查询口径
2. 输出直接标记、规则推断、未知编码、多编码、现金划转等统计口径
3. 输出 orgId、period、currency 过滤口径
4. 输出索引与性能优化建议
5. 对 BUS 未明确项显式标记“待确认”
