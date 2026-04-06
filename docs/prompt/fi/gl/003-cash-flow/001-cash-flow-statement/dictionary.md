# 现金流量表数据字典提示词

## 目标
根据 `docs/bus/fi/gl/003-cash-flow/001-cash-flow-statement/` 下业务文档，输出现金流量表场景的数据字典、统计口径、校验口径、告警口径与跳转口径，作为接口、前后端、测试的统一语义基础。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`

同时结合当前已确认实现：
- 后端服务：`BizfiFiCashFlowServiceImpl.query`
- 数据来源：已过账总账分录、现金流项目主数据、报表模板和报表项目
- 前端页面：`CashFlowView.vue`
- 接口：`GET /cash-flow`

## 输出要求
1. 输出查询条件字段字典
2. 输出报表行字段字典
3. 输出 `checks`、`warnings`、模板相关字段口径
4. 输出经营/投资/筹资活动与净增加额统计口径
5. 输出跳转按钮参数口径与待确认项
