# 往来多维分析表接口提示词

## 目标
根据 `docs/bus/fi/gl/004-counterparty-management/007-counterparty-multi-analysis/` 下业务文档，输出往来多维分析表场景的接口契约提示词，为前后端联调、测试与评审提供统一接口口径。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`
- `docs/prompt/fi/gl/004-counterparty-management/007-counterparty-multi-analysis/dictionary.md`

同时结合当前已确认实现：
- `GET /arap-manage/multi-analysis`
- 返回 `Map<String,Object>`
- 前端 API：`arapManageApi.fetchMultiAnalysis`
- 页面：`CounterpartyMultiAnalysisView.vue`

## 输出要求
1. 输出查询接口契约
2. 输出查询参数、返回结构、warnings 与统计字段口径
3. 输出 `rows` 的结构与 groupDimension 统计口径说明
4. 输出页面只读分析边界说明
5. 对 BUS 未明确项显式标记“待确认”
