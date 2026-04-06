# 往来通知单接口提示词

## 目标
根据 `docs/bus/fi/gl/005-internal-notice/001-counterparty-notice/` 下业务文档，输出往来通知单场景的接口契约提示词，为前后端联调、测试与评审提供统一接口口径。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`
- `docs/prompt/fi/gl/005-internal-notice/001-counterparty-notice/dictionary.md`

同时结合当前已确认实现：
- 查询接口：`GET /internal-notice/counterparty`
- 生成接口：`POST /internal-notice/counterparty/generate`
- 返回 `Map<String,Object>`
- 前端 API：`internalNoticeApi.fetchCounterpartyNotices / generateCounterpartyNotices`
- 页面：`CounterpartyNoticeView.vue`

## 输出要求
1. 输出查询接口与生成接口契约
2. 输出查询参数、生成参数、返回结构、warnings 与统计字段口径
3. 输出 `rows` 的结构与生成结果统计口径说明
4. 输出跳转到对账单与勾稽页面的参数映射说明
5. 对 BUS 未明确项显式标记“待确认”
