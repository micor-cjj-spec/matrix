# 往来通知单前端提示词

## 目标
结合 `docs/bus/fi/gl/005-internal-notice/001-counterparty-notice/` 下业务文档，以及当前前端已确认实现，输出往来通知单场景的前端实现建议与补齐提示词。

## 输入上下文
请结合以下文档：
- `page.md`
- `fields.md`
- `flow.md`
- `api.md`
- `docs/prompt/fi/gl/005-internal-notice/001-counterparty-notice/dictionary.md`
- `docs/prompt/fi/gl/005-internal-notice/001-counterparty-notice/api.md`
- `docs/prompt/fi/gl/005-internal-notice/001-counterparty-notice/backend.md`

同时结合当前已确认实现：
- 页面：`CounterpartyNoticeView.vue`
- 查询区：往来类型、通知状态、紧急程度、统计日期
- 汇总卡片、成功提示区、告警区、表格区
- 点击“查询”调用查询接口
- 点击“生成通知单”调用生成接口
- 行内可跳转到往来对账单或通知单勾稽页面

## 输出要求
1. 输出查询区、汇总卡片、成功提示区、告警区、表格区交互建议
2. 说明查询、生成通知单行为与空结果、异常态展示
3. 说明 warnings、生成成功信息、通知状态、紧急程度展示逻辑
4. 说明跳转到对账单与勾稽页面的参数映射
5. 对 BUS 未明确项显式标记“待确认”
