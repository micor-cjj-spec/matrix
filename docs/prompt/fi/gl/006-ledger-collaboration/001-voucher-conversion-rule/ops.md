# 凭证折算规则运维提示词

## 目标
根据 `docs/bus/fi/gl/006-ledger-collaboration/001-voucher-conversion-rule/` 下业务文档，以及当前已确认实现，输出凭证折算规则场景的发布、运维、监控与问题处理提示词。

## 输入上下文
请结合以下文档：
- `flow.md`
- `rules.md`
- `api.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/001-voucher-conversion-rule/backend.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/001-voucher-conversion-rule/frontend.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/001-voucher-conversion-rule/sql.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/001-voucher-conversion-rule/test.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/001-voucher-conversion-rule/review.md`

## 输出要求
1. 输出发布依赖检查项（前端/后端/规则配置依赖）
2. 输出规则查询、warnings、覆盖率与待生成量相关监控关注点
3. 输出跳转到应收/应付业务单据页面兼容性注意项
4. 输出回滚与异常排查建议
5. 对 BUS 未明确项显式标记“待确认”
