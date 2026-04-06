# 往来通知单勾稽接口提示词

## 目标
根据 `docs/bus/fi/gl/005-internal-notice/002-counterparty-notice-check/` 下业务文档，输出往来通知单勾稽场景的接口契约提示词，为前后端联调、测试与评审提供统一接口口径。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`
- `docs/prompt/fi/gl/005-internal-notice/002-counterparty-notice-check/dictionary.md`

同时结合当前已确认实现：
- `GET /internal-notice/counterparty/reconcile`
- 返回 `Map<String,Object>`
- 前端 API：`internalNoticeApi.fetchCounterpartyNoticeReconcile`
- 页面：`CounterpartyNoticeCheckView.vue`

## 输出要求
1. 输出查询接口契约
2. 输出查询参数、返回结构、warnings 与统计字段口径
3. 输出 `rows` 的结构与勾稽统计口径说明
4. 输出跳转到往来对账单时 `openOnly` 口径说明
5. 对 BUS 未明确项显式标记“待确认”
