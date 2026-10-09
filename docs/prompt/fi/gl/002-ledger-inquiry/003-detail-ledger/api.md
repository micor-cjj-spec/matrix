# 明细分类账接口提示词

## 目标
根据 `docs/bus/fi/gl/002-ledger-inquiry/003-detail-ledger/` 下业务文档，输出明细分类账场景的接口契约提示词，为前后端联调、测试与评审提供统一接口口径。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/003-detail-ledger/dictionary.md`

同时结合当前已确认实现：
- `GET /ledger/detail-ledger`
- 返回 `LedgerQueryResultVO`
- 前端 API：`ledgerApi.fetchDetailLedger`
- 页面：`DetailLedgerView.vue`

## 输出要求
1. 输出查询接口契约
2. 输出查询参数、返回结构、warnings 与 summary 字段口径
3. 输出 `records` 的结构与流水统计口径说明
4. 输出页面自动查询与查询参数映射说明
5. 对 BUS 未明确项显式标记“待确认”
