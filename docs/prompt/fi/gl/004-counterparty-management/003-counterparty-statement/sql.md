# 往来对账单SQL提示词

## 目标
根据 `docs/bus/fi/gl/004-counterparty-management/003-counterparty-statement/` 下业务文档，以及当前已确认实现，输出往来对账单场景的 SQL 与查询设计提示词。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `rules.md`
- `api.md`
- `docs/prompt/fi/gl/004-counterparty-management/003-counterparty-statement/dictionary.md`
- `docs/prompt/fi/gl/004-counterparty-management/003-counterparty-statement/backend.md`

同时结合当前已确认实现：
- 基于往来单据和已落库核销日志/链接生成结果
- 需要按 docTypeRoot、counterparty、asOfDate、openOnly 过滤

## 输出要求
1. 输出 `rows`、`recentLogs`、`warnings` 与统计字段所需查询口径
2. 输出往来单据、核销链接、核销日志关联口径
3. 输出原额、已核销金额、未核销金额、核销率相关数据来源建议
4. 输出索引与性能优化建议
5. 对 BUS 未明确项显式标记“待确认”
