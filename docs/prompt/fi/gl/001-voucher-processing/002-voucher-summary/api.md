# 凭证汇总表接口提示词

## 目标
根据 `docs/bus/fi/gl/001-voucher-processing/002-voucher-summary/` 下业务文档，输出凭证汇总表场景的接口契约提示词，为前后端联调、测试与评审提供统一接口口径。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`
- `docs/prompt/fi/gl/001-voucher-processing/002-voucher-summary/dictionary.md`

同时结合当前已确认实现：
- `GET /voucher/summary`
- 返回 `VoucherSummaryResultVO`
- 前端 API：`voucherApi.fetchSummary`
- 点击“查看凭证”跳转 `/ledger/voucher`

## 输出要求
1. 输出查询接口契约
2. 输出查询参数、返回结构、warnings 字段口径
3. 输出 rows 的结构与统计口径说明
4. 输出反查跳转参数约定
5. 对 BUS 未明确项显式标记“待确认”
