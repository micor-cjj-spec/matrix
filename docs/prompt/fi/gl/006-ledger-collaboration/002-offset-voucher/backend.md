# 对冲凭证后端提示词

## 目标
结合 `docs/bus/fi/gl/006-ledger-collaboration/002-offset-voucher/` 下业务文档，以及当前后端已确认实现，输出对冲凭证场景的后端实现建议与补齐提示词。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `api.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/002-offset-voucher/dictionary.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/002-offset-voucher/api.md`

同时结合当前已确认实现：
- 查询服务：`BizfiFiLedgerCollaborationServiceImpl.offsetVouchers`
- 基于凭证表中的冲销编号和备注关联信息进行配对
- 返回 `rows`、`warnings` 与统计字段

## 输出要求
1. 说明当前后端对冲链路分析能力与待补齐项
2. 输出原凭证、对冲凭证、金额差异、孤儿记录、配对状态等计算逻辑建议
3. 说明 startDate、endDate、matchStatus、keyword 参数处理逻辑
4. 说明性能、空结果与异常结果处理建议
5. 对 BUS 未明确项显式标记“待确认”
