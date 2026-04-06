# 现金流通知单SQL提示词

## 目标
根据 `docs/bus/fi/gl/005-internal-notice/003-cashflow-notice/` 下业务文档，以及当前已确认实现，输出现金流通知单场景的 SQL 与查询设计提示词。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `rules.md`
- `api.md`
- `docs/prompt/fi/gl/005-internal-notice/003-cashflow-notice/dictionary.md`
- `docs/prompt/fi/gl/005-internal-notice/003-cashflow-notice/backend.md`

同时结合当前已确认实现：
- 基于现金流补充资料待处理凭证与内部通知单表生成和查询结果
- 需要按 orgId、period、status、severity、sourceCode、currency 过滤

## 输出要求
1. 输出 `rows`、`warnings` 与统计字段所需查询口径
2. 输出通知单表、待处理凭证来源、问题来源与状态关联口径
3. 输出问题金额、未解决占比、生成结果相关数据来源建议
4. 输出索引与性能优化建议
5. 对 BUS 未明确项显式标记“待确认”
