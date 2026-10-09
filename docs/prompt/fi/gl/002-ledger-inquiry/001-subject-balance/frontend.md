# 科目余额表前端提示词

## 目标
结合 `docs/bus/fi/gl/002-ledger-inquiry/001-subject-balance/` 下业务文档，以及当前前端已确认实现，输出科目余额表场景的前端实现建议与补齐提示词。

## 输入上下文
请结合以下文档：
- `page.md`
- `fields.md`
- `flow.md`
- `api.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/001-subject-balance/dictionary.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/001-subject-balance/api.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/001-subject-balance/backend.md`

同时结合当前已确认实现：
- 页面：`SubjectBalanceView.vue`
- 查询区：开始日期、结束日期、科目编码
- 汇总卡片、warnings 告警区、表格区
- 路由 query 可带入 `startDate / endDate / accountCode`
- 科目下拉通过共享方法加载会计科目选项

## 输出要求
1. 输出查询区、汇总卡片、告警区、表格区交互建议
2. 说明默认查询、query 回填、查询/重置行为
3. 说明科目下拉加载、空结果展示、异常态展示
4. 说明 warnings 展示与 summary 展示逻辑
5. 对 BUS 未明确项显式标记“待确认”
