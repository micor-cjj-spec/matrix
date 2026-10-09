# 凭证协同检查后端提示词

## 目标
结合 `docs/bus/fi/gl/006-ledger-collaboration/003-voucher-collaboration-check/` 下业务文档，以及当前后端已确认实现，输出凭证协同检查场景的后端实现建议与补齐提示词。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `api.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/003-voucher-collaboration-check/dictionary.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/003-voucher-collaboration-check/api.md`

同时结合当前已确认实现：
- 查询服务：`BizfiFiLedgerCollaborationServiceImpl.voucherChecks`
- 基于凭证、凭证分录、总账分录联合检查生成结果
- 返回 `rows`、`warnings` 与统计字段

## 输出要求
1. 说明当前后端协同检查能力与待补齐项
2. 输出借贷不平、缺少分录、缺少总账分录、重复凭证号等问题识别逻辑建议
3. 说明 startDate、endDate、issueCode、severity、status、onlyIssue 参数处理逻辑
4. 说明性能、空结果与异常结果处理建议
5. 对 BUS 未明确项显式标记“待确认”
