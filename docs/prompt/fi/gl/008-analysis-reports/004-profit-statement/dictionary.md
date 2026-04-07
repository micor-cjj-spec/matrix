# 利润表数据字典提示词

## 目标
根据 `docs/bus/fi/gl/008-analysis-reports/004-profit-statement/` 下业务文档，输出利润表场景的数据字典、报表口径、期间口径、校验口径与展示口径，作为前端、接口、测试的统一语义基础。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`

同时结合当前已确认实现：
- 前端页面：`ProfitStatementView.vue`
- 前端 API：`financialReportApi.fetchProfitStatement`
- 查询接口：`GET /profit-statement`

## 输出要求
1. 输出查询条件字段字典
2. 输出告警区、校验区、主表区字段字典
3. 输出本期金额、本年累计金额、warnings、checks 的口径
4. 输出 startPeriod/endPeriod 与 showZero 对展示的影响
5. 对未明确的后端内部实现统一标记“待确认”
