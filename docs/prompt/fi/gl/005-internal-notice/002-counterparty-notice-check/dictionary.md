# 往来通知单勾稽数据字典提示词

## 目标
根据 `docs/bus/fi/gl/005-internal-notice/002-counterparty-notice-check/` 下业务文档，输出往来通知单勾稽场景的数据字典、统计口径、勾稽状态口径、告警口径与跳转口径，作为接口、前后端、测试的统一语义基础。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`

同时结合当前已确认实现：
- 后端服务：`BizfiFiInternalNoticeServiceImpl.reconcileCounterpartyNotices`
- 前端页面：`CounterpartyNoticeCheckView.vue`
- 接口：`GET /internal-notice/counterparty/reconcile`

## 输出要求
1. 输出查询条件字段字典
2. 输出汇总卡片字段字典
3. 输出勾稽结果清单字段字典
4. 输出通知数、仍需处理、通知快照未清、当前未清、勾稽状态等统计口径
5. 输出 warnings 与跳转到往来对账单口径
6. 对 BUS 未明确项显式标记“待确认”
