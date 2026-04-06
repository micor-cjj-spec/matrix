# 往来多维分析表前端提示词

## 目标
结合 `docs/bus/fi/gl/004-counterparty-management/007-counterparty-multi-analysis/` 下业务文档，以及当前前端已确认实现，输出往来多维分析表场景的前端实现建议与补齐提示词。

## 输入上下文
请结合以下文档：
- `page.md`
- `fields.md`
- `flow.md`
- `api.md`
- `docs/prompt/fi/gl/004-counterparty-management/007-counterparty-multi-analysis/dictionary.md`
- `docs/prompt/fi/gl/004-counterparty-management/007-counterparty-multi-analysis/api.md`
- `docs/prompt/fi/gl/004-counterparty-management/007-counterparty-multi-analysis/backend.md`

同时结合当前已确认实现：
- 页面：`CounterpartyMultiAnalysisView.vue`
- 查询区：往来类型、汇总维度、统计日期
- 汇总卡片、告警区、表格区
- 当前页面只做汇总分析，不直接下钻到执行动作

## 输出要求
1. 输出查询区、汇总卡片、告警区、表格区交互建议
2. 说明查询行为、空结果与异常态展示
3. 说明 warnings、统计字段、groupDimension 展示逻辑
4. 说明页面只读分析边界
5. 对 BUS 未明确项显式标记“待确认”
