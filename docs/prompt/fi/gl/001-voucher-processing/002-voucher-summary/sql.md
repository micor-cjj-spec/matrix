# 凭证汇总表SQL提示词

## 目标
根据 `docs/bus/fi/gl/001-voucher-processing/002-voucher-summary/` 下业务文档，以及当前已确认实现，输出凭证汇总表场景的 SQL 与查询设计提示词。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `rules.md`
- `api.md`
- `docs/prompt/fi/gl/001-voucher-processing/002-voucher-summary/dictionary.md`
- `docs/prompt/fi/gl/001-voucher-processing/002-voucher-summary/backend.md`

同时结合当前已确认实现：
- 汇总直接基于 `BizfiFiVoucher` 主表聚合
- 不读取分录表
- 需要按日期、状态、摘要条件统计

## 输出要求
1. 输出主表聚合查询口径
2. 输出日期、状态、摘要关键字查询条件建议
3. 输出日期分组、状态计数、金额汇总的 SQL 设计建议
4. 输出索引与性能优化建议
5. 对 BUS 未明确项显式标记“待确认”
