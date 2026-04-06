# 往来核销方案前端提示词

## 目标
结合 `docs/bus/fi/gl/004-counterparty-management/001-counterparty-writeoff-plan/` 下业务文档，以及当前前端已确认实现，输出往来核销方案场景的前端实现建议与补齐提示词。

## 输入上下文
请结合以下文档：
- `page.md`
- `fields.md`
- `flow.md`
- `api.md`
- `docs/prompt/fi/gl/004-counterparty-management/001-counterparty-writeoff-plan/dictionary.md`
- `docs/prompt/fi/gl/004-counterparty-management/001-counterparty-writeoff-plan/api.md`
- `docs/prompt/fi/gl/004-counterparty-management/001-counterparty-writeoff-plan/backend.md`

同时结合当前已确认实现：
- 页面：`CounterpartyPlanView.vue`
- 查询区：往来类型、往来方、统计日期、仅已审核
- 汇总卡片、告警区、表格区
- 点击“去自动核销”跳转自动核销页并带当前查询条件

## 输出要求
1. 输出查询区、汇总卡片、告警区、表格区交互建议
2. 说明查询方案行为与空结果、异常态展示
3. 说明 warnings 与统计字段展示逻辑
4. 说明“去自动核销”跳转参数映射
5. 对 BUS 未明确项显式标记“待确认”
