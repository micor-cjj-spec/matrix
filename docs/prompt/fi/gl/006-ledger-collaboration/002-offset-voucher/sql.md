# 对冲凭证SQL提示词

## 目标
根据 `docs/bus/fi/gl/006-ledger-collaboration/002-offset-voucher/` 下业务文档，以及当前已确认实现，输出对冲凭证场景的 SQL 与查询设计提示词。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `rules.md`
- `api.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/002-offset-voucher/dictionary.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/002-offset-voucher/backend.md`

同时结合当前已确认实现：
- 基于凭证表中的冲销编号和备注关联信息进行配对
- 需要按 startDate、endDate、matchStatus、keyword 过滤

## 输出要求
1. 输出 `rows`、`warnings` 与统计字段所需查询口径
2. 输出原凭证、对冲凭证、编号链路、备注链路关联口径
3. 输出已配对、孤儿记录、原凭证金额、对冲金额、金额差异相关数据来源建议
4. 输出索引与性能优化建议
5. 对 BUS 未明确项显式标记“待确认”
