# 对冲凭证运维提示词

## 目标
根据 `docs/bus/fi/gl/006-ledger-collaboration/002-offset-voucher/` 下业务文档，以及当前已确认实现，输出对冲凭证场景的发布、运维、监控与问题处理提示词。

## 输入上下文
请结合以下文档：
- `flow.md`
- `rules.md`
- `api.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/002-offset-voucher/backend.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/002-offset-voucher/frontend.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/002-offset-voucher/sql.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/002-offset-voucher/test.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/002-offset-voucher/review.md`

## 输出要求
1. 输出发布依赖检查项（前端/后端/凭证链路依赖）
2. 输出对冲凭证查询、warnings、孤儿记录与金额差异相关监控关注点
3. 输出跳转查看原凭证/对冲凭证兼容性注意项
4. 输出回滚与异常排查建议
5. 对 BUS 未明确项显式标记“待确认”
