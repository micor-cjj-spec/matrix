# 科目余额对照前端提示词

## 目标
结合 `docs/bus/fi/gl/006-ledger-collaboration/004-subject-compare/` 下业务文档，以及当前前端已确认实现，输出科目余额对照场景的前端实现建议与补齐提示词。

## 输入上下文
请结合以下文档：
- `page.md`
- `fields.md`
- `flow.md`
- `api.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/004-subject-compare/dictionary.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/004-subject-compare/api.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/004-subject-compare/backend.md`

同时结合当前已确认实现：
- 页面：`SubjectCompareView.vue`
- 查询区：开始日期、结束日期、科目编码、仅看差异
- 汇总卡片、告警区、表格区
- 初始化时加载科目选项并自动查询
- 点击“查询”调用科目余额对照接口
- 行内可跳转到科目余额表页面

## 输出要求
1. 输出查询区、汇总卡片、告警区、表格区交互建议
2. 说明初始化自动查询、查询行为与空结果、异常态展示
3. 说明 warnings、借贷差值、余额差额、仅看差异展示逻辑
4. 说明跳转到科目余额表页面的参数映射
5. 对 BUS 未明确项显式标记“待确认”
