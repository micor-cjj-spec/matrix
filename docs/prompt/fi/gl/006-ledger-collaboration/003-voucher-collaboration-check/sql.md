# 凭证协同检查SQL提示词

## 目标
根据 `docs/bus/fi/gl/006-ledger-collaboration/003-voucher-collaboration-check/` 下业务文档，以及当前已确认实现，输出凭证协同检查场景的 SQL 与查询设计提示词。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `rules.md`
- `api.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/003-voucher-collaboration-check/dictionary.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/003-voucher-collaboration-check/backend.md`

同时结合当前已确认实现：
- 基于凭证、凭证分录、总账分录联合检查生成结果
- 需要按 startDate、endDate、issueCode、severity、status、onlyIssue 过滤

## 输出要求
1. 输出 `rows`、`warnings` 与统计字段所需查询口径
2. 输出凭证表、凭证分录、总账分录关联口径
3. 输出扫描凭证数、异常条数、高风险问题、健康凭证、问题类型相关数据来源建议
4. 输出索引与性能优化建议
5. 对 BUS 未明确项显式标记“待确认”
