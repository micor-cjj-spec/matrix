# 往来通知单勾稽运维提示词

## 目标
根据 `docs/bus/fi/gl/005-internal-notice/002-counterparty-notice-check/` 下业务文档，以及当前已确认实现，输出往来通知单勾稽场景的发布、运维、监控与问题处理提示词。

## 输入上下文
请结合以下文档：
- `flow.md`
- `rules.md`
- `api.md`
- `docs/prompt/fi/gl/005-internal-notice/002-counterparty-notice-check/backend.md`
- `docs/prompt/fi/gl/005-internal-notice/002-counterparty-notice-check/frontend.md`
- `docs/prompt/fi/gl/005-internal-notice/002-counterparty-notice-check/sql.md`
- `docs/prompt/fi/gl/005-internal-notice/002-counterparty-notice-check/test.md`
- `docs/prompt/fi/gl/005-internal-notice/002-counterparty-notice-check/review.md`

## 输出要求
1. 输出发布依赖检查项（前端/后端/风险候选结果依赖）
2. 输出勾稽查询、warnings、`openOnly` 跳转相关监控关注点
3. 输出跳转到往来对账单的兼容性注意项
4. 输出回滚与异常排查建议
5. 对 BUS 未明确项显式标记“待确认”
