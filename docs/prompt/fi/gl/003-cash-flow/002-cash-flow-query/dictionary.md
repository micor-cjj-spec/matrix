# 现金流量查询数据字典提示词

## 目标
根据 `docs/bus/fi/gl/003-cash-flow/002-cash-flow-query/` 下业务文档，输出现金流量查询场景的数据字典、统计口径、识别来源口径、告警口径与跳转口径，作为接口、前后端、测试的统一语义基础。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`

同时结合当前已确认实现：
- 后端服务：`BizfiFiCashFlowServiceImpl.trace`
- 前端页面：`CashFlowQueryView.vue`
- 接口：`GET /cash-flow/query`

## 输出要求
1. 输出查询条件字段字典
2. 输出汇总卡片、状态条字段字典
3. 输出现金流追踪记录字段字典
4. 输出直接标记、规则推断、未知编码、多编码复核、现金划转等口径
5. 输出 warnings 与查看凭证跳转口径
6. 对 BUS 未明确项显式标记“待确认”
