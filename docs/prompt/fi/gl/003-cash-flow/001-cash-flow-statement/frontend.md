# 现金流量表前端提示词

## 目标
结合 `docs/bus/fi/gl/003-cash-flow/001-cash-flow-statement/` 下业务文档，以及当前前端已确认实现，输出现金流量表场景的前端实现建议与补齐提示词。

## 输入上下文
请结合以下文档：
- `page.md`
- `fields.md`
- `flow.md`
- `api.md`
- `docs/prompt/fi/gl/003-cash-flow/001-cash-flow-statement/dictionary.md`
- `docs/prompt/fi/gl/003-cash-flow/001-cash-flow-statement/api.md`
- `docs/prompt/fi/gl/003-cash-flow/001-cash-flow-statement/backend.md`

同时结合当前已确认实现：
- 页面：`CashFlowView.vue`
- 查询区：业务单元、期间、币种、模板ID、显示零值行
- 告警区、校验区、表格区
- 跳转按钮：现金流量查询、补充资料
- 初始化时加载业务单元并自动查询

## 输出要求
1. 输出查询区、告警区、校验区、表格区交互建议
2. 说明初始化自动查询、查询行为、showZero 展示逻辑
3. 说明 checks、warnings、模板信息展示逻辑
4. 说明跳转按钮到 `/ledger/cash-flow-query` 与 `/ledger/cash-flow-supplement` 的参数映射
5. 对 BUS 未明确项显式标记“待确认”
