# 往来通知单数据字典提示词

## 目标
根据 `docs/bus/fi/gl/005-internal-notice/001-counterparty-notice/` 下业务文档，输出往来通知单场景的数据字典、统计口径、通知状态口径、生成口径与跳转口径，作为接口、前后端、测试的统一语义基础。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`

同时结合当前已确认实现：
- 后端服务：`BizfiFiInternalNoticeServiceImpl.generateCounterpartyNotices / queryCounterpartyNotices`
- 前端页面：`CounterpartyNoticeView.vue`
- 查询接口：`GET /internal-notice/counterparty`
- 生成接口：`POST /internal-notice/counterparty/generate`

## 输出要求
1. 输出查询条件字段字典
2. 输出汇总卡片、成功提示区字段字典
3. 输出通知清单字段字典
4. 输出通知状态、紧急程度、通知金额、未清金额、生成结果统计口径
5. 输出 warnings 与跳转到对账单/勾稽页面口径
6. 对 BUS 未明确项显式标记“待确认”
