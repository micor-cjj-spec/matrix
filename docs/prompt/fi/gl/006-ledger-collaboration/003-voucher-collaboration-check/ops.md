# 凭证协同检查运维提示词

## 目标
根据 `docs/bus/fi/gl/006-ledger-collaboration/003-voucher-collaboration-check/` 下业务文档，以及当前已确认实现，输出凭证协同检查场景的发布、运维、监控与问题处理提示词。

## 输入上下文
请结合以下文档：
- `flow.md`
- `rules.md`
- `api.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/003-voucher-collaboration-check/backend.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/003-voucher-collaboration-check/frontend.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/003-voucher-collaboration-check/sql.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/003-voucher-collaboration-check/test.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/003-voucher-collaboration-check/review.md`

## 输出要求
1. 输出发布依赖检查项（前端/后端/凭证链路依赖）
2. 输出协同检查查询、warnings、问题类型与高风险问题相关监控关注点
3. 输出跳转查看凭证兼容性注意项
4. 输出回滚与异常排查建议
5. 对 BUS 未明确项显式标记“待确认”
