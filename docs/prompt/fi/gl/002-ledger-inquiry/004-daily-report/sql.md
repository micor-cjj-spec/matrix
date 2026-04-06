# 日报表SQL提示词

## 目标
根据 `docs/bus/fi/gl/002-ledger-inquiry/004-daily-report/` 下业务文档，以及当前已确认实现，输出日报表场景的 SQL 与查询设计提示词。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `rules.md`
- `api.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/004-daily-report/dictionary.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/004-daily-report/backend.md`

同时结合当前已确认实现：
- 基于已过账总账分录 `BizfiFiGlEntry` 按日聚合
- 需要按日期区间、科目编码生成日级统计视图

## 输出要求
1. 输出按日借方、贷方、凭证数、滚动余额的查询口径
2. 输出日期区间与科目编码过滤口径
3. 输出 summary 汇总计算建议
4. 输出索引与性能优化建议
5. 对 BUS 未明确项显式标记“待确认”
