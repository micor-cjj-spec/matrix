# 现金流通知单后端提示词

## 目标
结合 `docs/bus/fi/gl/005-internal-notice/003-cashflow-notice/` 下业务文档，以及当前后端已确认实现，输出现金流通知单场景的后端实现建议与补齐提示词。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `api.md`
- `docs/prompt/fi/gl/005-internal-notice/003-cashflow-notice/dictionary.md`
- `docs/prompt/fi/gl/005-internal-notice/003-cashflow-notice/api.md`

同时结合当前已确认实现：
- 查询服务：`BizfiFiInternalNoticeServiceImpl.queryCashflowNotices`
- 生成服务：`BizfiFiInternalNoticeServiceImpl.generateCashflowNotices`
- 基于现金流补充资料待处理凭证与内部通知单表生成和查询结果

## 输出要求
1. 说明当前后端通知生成与查询能力与待补齐项
2. 输出通知状态、紧急程度、问题来源、问题金额、自动关闭等逻辑建议
3. 说明 orgId、period、status、severity、sourceCode、currency、operator 参数处理逻辑
4. 说明生成幂等、插入/更新/自动关闭、性能、空结果与异常结果处理建议
5. 对 BUS 未明确项显式标记“待确认”
