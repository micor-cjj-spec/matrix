# 现金流量查询SQL提示词

## 目标
根据 `docs/bus/fi/gl/003-cash-flow/002-cash-flow-query/` 下业务文档，以及当前已确认实现，输出现金流量查询场景的 SQL 与查询设计提示词。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `rules.md`
- `api.md`
- `docs/prompt/fi/gl/003-cash-flow/002-cash-flow-query/dictionary.md`
- `docs/prompt/fi/gl/003-cash-flow/002-cash-flow-query/backend.md`

同时结合当前已确认实现：
- 基于现金相关凭证分析结果生成追踪列表
- 需要按 orgId、period、currency、cashflowItemCode、categoryCode、sourceType、accountCode、keyword 过滤

## 输出要求
1. 输出 records、warnings、统计字段所需查询口径
2. 输出识别来源、活动分类、现金流项目等过滤口径
3. 输出未知编码、多编码复核、现金划转等数据来源建议
4. 输出索引与性能优化建议
5. 对 BUS 未明确项显式标记“待确认”
