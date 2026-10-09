# 报表项目SQL提示词

## 目标
根据 `docs/bus/fi/gl/008-analysis-reports/001-report-item/` 下业务文档，以及当前已确认实现，输出报表项目场景的 SQL 与查询设计提示词。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `rules.md`
- `api.md`
- `docs/prompt/fi/gl/008-analysis-reports/001-report-item/dictionary.md`
- `docs/prompt/fi/gl/008-analysis-reports/001-report-item/backend.md`

同时结合当前已确认实现：
- 基于报表项目表完成分页查询、树查询、详情与基础维护
- 需要按模板、项目编码、项目名称过滤

## 输出要求
1. 输出 list/tree/detail 所需查询口径
2. 输出模板维度、项目树层级、排序口径
3. 输出写操作涉及的唯一性与层级一致性建议
4. 输出索引与性能优化建议
5. 对 BUS 未明确项显式标记“待确认”
