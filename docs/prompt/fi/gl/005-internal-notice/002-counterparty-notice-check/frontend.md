# 往来通知单勾稽前端提示词

## 目标
结合 `docs/bus/fi/gl/005-internal-notice/002-counterparty-notice-check/` 下业务文档，以及当前前端已确认实现，输出往来通知单勾稽场景的前端实现建议与补齐提示词。

## 输入上下文
请结合以下文档：
- `page.md`
- `fields.md`
- `flow.md`
- `api.md`
- `docs/prompt/fi/gl/005-internal-notice/002-counterparty-notice-check/dictionary.md`
- `docs/prompt/fi/gl/005-internal-notice/002-counterparty-notice-check/api.md`
- `docs/prompt/fi/gl/005-internal-notice/002-counterparty-notice-check/backend.md`

同时结合当前已确认实现：
- 页面：`CounterpartyNoticeCheckView.vue`
- 查询区：往来类型、通知状态、统计日期
- 汇总卡片、告警区、表格区
- 点击“查询”调用勾稽接口
- 行内可跳转到往来对账单，并按勾稽结果决定是否带 `openOnly=true`

## 输出要求
1. 输出查询区、汇总卡片、告警区、表格区交互建议
2. 说明查询行为与空结果、异常态展示
3. 说明 warnings、勾稽状态、快照未清与当前未清展示逻辑
4. 说明跳转到往来对账单时 `openOnly` 的参数映射
5. 对 BUS 未明确项显式标记“待确认”
