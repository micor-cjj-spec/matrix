# 现金流量表接口提示词

## 目标
根据 `docs/bus/fi/gl/003-cash-flow/001-cash-flow-statement/` 下业务文档，输出现金流量表场景的接口契约提示词，为前后端联调、测试与评审提供统一接口口径。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`
- `docs/prompt/fi/gl/003-cash-flow/001-cash-flow-statement/dictionary.md`

同时结合当前已确认实现：
- `GET /cash-flow`
- 返回 `ReportQueryResultVO`
- 前端 API：`financialReportApi.fetchCashFlow`
- 页面：`CashFlowView.vue`

## 输出要求
1. 输出查询接口契约
2. 输出查询参数、返回结构、checks 与 warnings 字段口径
3. 输出 `rows` 的结构与报表统计口径说明
4. 输出页面自动查询与跳转按钮参数映射说明
5. 对 BUS 未明确项显式标记“待确认”
