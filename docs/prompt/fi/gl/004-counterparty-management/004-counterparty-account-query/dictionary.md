# 往来账查询数据字典提示词

## 目标
根据 `docs/bus/fi/gl/004-counterparty-management/004-counterparty-account-query/` 下业务文档，输出往来账查询场景的数据字典、统计口径、核销口径、告警口径与展示口径，作为接口、前后端、测试的统一语义基础。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`

同时结合当前已确认实现：
- 后端服务：`BizfiFiArapManageServiceImpl.accountQuery`
- 前端页面：`CounterpartyAccountQueryView.vue`
- 接口：`GET /arap-manage/account-query`

## 输出要求
1. 输出查询条件字段字典
2. 输出汇总卡片字段字典
3. 输出单据级查询结果字段字典
4. 输出原额、已核销、未核销、核销状态、账龄等口径
5. 输出 warnings 与关联凭证展示口径
6. 对 BUS 未明确项显式标记“待确认”
