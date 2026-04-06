# 往来通知单勾稽后端提示词

## 目标
结合 `docs/bus/fi/gl/005-internal-notice/002-counterparty-notice-check/` 下业务文档，以及当前后端已确认实现，输出往来通知单勾稽场景的后端实现建议与补齐提示词。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `api.md`
- `docs/prompt/fi/gl/005-internal-notice/002-counterparty-notice-check/dictionary.md`
- `docs/prompt/fi/gl/005-internal-notice/002-counterparty-notice-check/api.md`

同时结合当前已确认实现：
- 查询服务：`BizfiFiInternalNoticeServiceImpl.reconcileCounterpartyNotices`
- 基于内部通知单表与最新账龄风险候选结果生成勾稽结果
- 返回 `rows`、`warnings` 与统计字段

## 输出要求
1. 说明当前后端勾稽能力与待补齐项
2. 输出仍需处理、已自然解除、快照未清与当前未清对比逻辑建议
3. 说明 docTypeRoot、status、asOfDate 参数处理逻辑
4. 说明性能、空结果与异常结果处理建议
5. 对 BUS 未明确项显式标记“待确认”
