# 往来通知单SQL提示词

## 目标
根据 `docs/bus/fi/gl/005-internal-notice/001-counterparty-notice/` 下业务文档，以及当前已确认实现，输出往来通知单场景的 SQL 与查询设计提示词。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `rules.md`
- `api.md`
- `docs/prompt/fi/gl/005-internal-notice/001-counterparty-notice/dictionary.md`
- `docs/prompt/fi/gl/005-internal-notice/001-counterparty-notice/backend.md`

同时结合当前已确认实现：
- 基于往来账龄分析结果与内部通知单表生成和查询结果
- 需要按 docTypeRoot、status、severity、asOfDate 过滤

## 输出要求
1. 输出 `rows`、`warnings` 与统计字段所需查询口径
2. 输出通知单表、账龄风险结果、状态与紧急程度关联口径
3. 输出通知金额、未清金额、生成结果相关数据来源建议
4. 输出索引与性能优化建议
5. 对 BUS 未明确项显式标记“待确认”
