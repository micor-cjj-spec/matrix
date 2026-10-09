# 账龄分析表SQL提示词

## 目标
根据 `docs/bus/fi/gl/004-counterparty-management/006-aging-analysis/` 下业务文档，以及当前已确认实现，输出账龄分析表场景的 SQL 与查询设计提示词。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `rules.md`
- `api.md`
- `docs/prompt/fi/gl/004-counterparty-management/006-aging-analysis/dictionary.md`
- `docs/prompt/fi/gl/004-counterparty-management/006-aging-analysis/backend.md`

同时结合当前已确认实现：
- 基于往来单据、核销链接、往来方信用额度主数据生成结果
- 需要按 docTypeRoot、asOfDate 过滤

## 输出要求
1. 输出 `rows`、`warnings` 与统计字段所需查询口径
2. 输出未核销余额、账龄分桶、信用额度、逾期阈值相关数据来源建议
3. 输出往来方与信用额度主数据关联口径
4. 输出索引与性能优化建议
5. 对 BUS 未明确项显式标记“待确认”
