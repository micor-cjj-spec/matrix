# 科目余额对照后端提示词

## 目标
结合 `docs/bus/fi/gl/006-ledger-collaboration/004-subject-compare/` 下业务文档，以及当前后端已确认实现，输出科目余额对照场景的后端实现建议与补齐提示词。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `api.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/004-subject-compare/dictionary.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/004-subject-compare/api.md`

同时结合当前已确认实现：
- 查询服务：`BizfiFiLedgerCollaborationServiceImpl.subjectBalanceCompare`
- 分别聚合凭证分录与总账分录后做科目级对照
- 返回 `rows`、`warnings` 与统计字段

## 输出要求
1. 说明当前后端科目级对照能力与待补齐项
2. 输出借方、贷方、余额、期初、发生、期末差异等计算逻辑建议
3. 说明 startDate、endDate、accountCode、diffOnly 参数处理逻辑
4. 说明性能、空结果与异常结果处理建议
5. 对 BUS 未明确项显式标记“待确认”
