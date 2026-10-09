# 往来自动核销前端提示词

## 目标
结合 `docs/bus/fi/gl/004-counterparty-management/002-counterparty-auto-writeoff/` 下业务文档，以及当前前端已确认实现，输出往来自动核销场景的前端实现建议与补齐提示词。

## 输入上下文
请结合以下文档：
- `page.md`
- `fields.md`
- `flow.md`
- `api.md`
- `docs/prompt/fi/gl/004-counterparty-management/002-counterparty-auto-writeoff/dictionary.md`
- `docs/prompt/fi/gl/004-counterparty-management/002-counterparty-auto-writeoff/api.md`
- `docs/prompt/fi/gl/004-counterparty-management/002-counterparty-auto-writeoff/backend.md`

同时结合当前已确认实现：
- 页面：`CounterpartyAutoWriteoffView.vue`
- 查询区：往来类型、往来方、统计日期
- 汇总卡片、成功提示区、告警区、表格区
- 初始化时可从方案页带入条件并自动预览方案
- 点击“预览方案”调用方案接口
- 点击“执行自动核销”调用执行接口

## 输出要求
1. 输出查询区、汇总卡片、成功提示区、告警区、表格区交互建议
2. 说明方案页条件带入、自动预览、预览方案与执行自动核销行为
3. 说明 warnings、执行成功信息、统计字段展示逻辑
4. 说明空结果、异常态与重复点击防护建议
5. 对 BUS 未明确项显式标记“待确认”
