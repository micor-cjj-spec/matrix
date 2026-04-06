# 科目余额对照SQL提示词

## 目标
根据 `docs/bus/fi/gl/006-ledger-collaboration/004-subject-compare/` 下业务文档，以及当前已确认实现，输出科目余额对照场景的 SQL 与查询设计提示词。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `rules.md`
- `api.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/004-subject-compare/dictionary.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/004-subject-compare/backend.md`

同时结合当前已确认实现：
- 分别聚合凭证分录与总账分录后做科目级对照
- 需要按 startDate、endDate、accountCode、diffOnly 过滤

## 输出要求
1. 输出 `rows`、`warnings` 与统计字段所需查询口径
2. 输出凭证分录汇总口径与总账分录汇总口径关联口径
3. 输出借方、贷方、余额、期初、发生、期末差异相关数据来源建议
4. 输出索引与性能优化建议
5. 对 BUS 未明确项显式标记“待确认”
