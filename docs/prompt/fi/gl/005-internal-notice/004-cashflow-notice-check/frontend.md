# 现金流通知单勾稽前端提示词

## 目标
结合 `docs/bus/fi/gl/005-internal-notice/004-cashflow-notice-check/` 下业务文档，以及当前前端已确认实现，输出现金流通知单勾稽场景的前端实现建议与补齐提示词。

## 输入上下文
请结合以下文档：
- `page.md`
- `fields.md`
- `flow.md`
- `api.md`
- `docs/prompt/fi/gl/005-internal-notice/004-cashflow-notice-check/dictionary.md`
- `docs/prompt/fi/gl/005-internal-notice/004-cashflow-notice-check/api.md`
- `docs/prompt/fi/gl/005-internal-notice/004-cashflow-notice-check/backend.md`

同时结合当前已确认实现：
- 页面：`CashflowNoticeCheckView.vue`
- 查询区：业务单元、期间、通知状态
- 汇总卡片、告警区、表格区
- 初始化时加载业务单元并自动查询
- 点击“查询”调用现金流通知单勾稽接口
- 行内可跳转到凭证页查看具体凭证

## 输出要求
1. 输出查询区、汇总卡片、告警区、表格区交互建议
2. 说明初始化自动查询、查询行为与空结果、异常态展示
3. 说明 warnings、勾稽状态、快照金额与当前金额展示逻辑
4. 说明跳转到凭证页的参数映射
5. 对 BUS 未明确项显式标记“待确认”
