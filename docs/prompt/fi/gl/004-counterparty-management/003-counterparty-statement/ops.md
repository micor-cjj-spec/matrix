# 往来对账单运维提示词

## 目标
根据 `docs/bus/fi/gl/004-counterparty-management/003-counterparty-statement/` 下业务文档，以及当前已确认实现，输出往来对账单场景的发布、运维、监控与问题处理提示词。

## 输入上下文
请结合以下文档：
- `flow.md`
- `rules.md`
- `api.md`
- `docs/prompt/fi/gl/004-counterparty-management/003-counterparty-statement/backend.md`
- `docs/prompt/fi/gl/004-counterparty-management/003-counterparty-statement/frontend.md`
- `docs/prompt/fi/gl/004-counterparty-management/003-counterparty-statement/sql.md`
- `docs/prompt/fi/gl/004-counterparty-management/003-counterparty-statement/test.md`
- `docs/prompt/fi/gl/004-counterparty-management/003-counterparty-statement/review.md`

## 输出要求
1. 输出发布依赖检查项（前端/后端/往来方与日志依赖）
2. 输出对账单查询、warnings、最近批次摘要相关监控关注点
3. 输出关联凭证跳转与核销日志跳转兼容性注意项
4. 输出回滚与异常排查建议
5. 对 BUS 未明确项显式标记“待确认”
