# 往来对账单前端提示词

## 目标
结合 `docs/bus/fi/gl/004-counterparty-management/003-counterparty-statement/` 下业务文档，以及当前前端已确认实现，输出往来对账单场景的前端实现建议与补齐提示词。

## 输入上下文
请结合以下文档：
- `page.md`
- `fields.md`
- `flow.md`
- `api.md`
- `docs/prompt/fi/gl/004-counterparty-management/003-counterparty-statement/dictionary.md`
- `docs/prompt/fi/gl/004-counterparty-management/003-counterparty-statement/api.md`
- `docs/prompt/fi/gl/004-counterparty-management/003-counterparty-statement/backend.md`

同时结合当前已确认实现：
- 页面：`CounterpartyStatementView.vue`
- 查询区：往来类型、往来方、统计日期、仅看未核销
- 汇总卡片、对账明细表、最近核销批次表、告警区
- 对账明细可跳转关联凭证
- 最近批次可跳转核销日志并带入方案号

## 输出要求
1. 输出查询区、汇总卡片、对账明细表、最近核销批次表、告警区交互建议
2. 说明查询行为、空结果与异常态展示
3. 说明 warnings、统计字段、核销状态展示逻辑
4. 说明关联凭证跳转与核销日志跳转参数映射
5. 对 BUS 未明确项显式标记“待确认”
