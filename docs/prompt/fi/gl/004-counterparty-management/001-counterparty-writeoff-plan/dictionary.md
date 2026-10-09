# 往来核销方案数据字典提示词

## 目标
根据 `docs/bus/fi/gl/004-counterparty-management/001-counterparty-writeoff-plan/` 下业务文档，输出往来核销方案场景的数据字典、统计口径、告警口径与跳转口径，作为接口、前后端、测试的统一语义基础。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`

同时结合当前已确认实现：
- 后端服务：`BizfiFiArapManageServiceImpl.plan`
- 前端页面：`CounterpartyPlanView.vue`
- 接口：`GET /arap-manage/plan`

## 输出要求
1. 输出查询条件字段字典
2. 输出汇总卡片字段字典
3. 输出核销建议方案记录字段字典
4. 输出源单据待核销、结算单据待分配、建议核销金额、方案后剩余等统计口径
5. 输出 warnings 与“去自动核销”跳转口径
6. 对 BUS 未明确项显式标记“待确认”
