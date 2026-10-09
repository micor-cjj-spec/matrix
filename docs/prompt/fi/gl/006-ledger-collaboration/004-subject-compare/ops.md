# 科目余额对照运维提示词

## 目标
根据 `docs/bus/fi/gl/006-ledger-collaboration/004-subject-compare/` 下业务文档，以及当前已确认实现，输出科目余额对照场景的发布、运维、监控与问题处理提示词。

## 输入上下文
请结合以下文档：
- `flow.md`
- `rules.md`
- `api.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/004-subject-compare/backend.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/004-subject-compare/frontend.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/004-subject-compare/sql.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/004-subject-compare/test.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/004-subject-compare/review.md`

## 输出要求
1. 输出发布依赖检查项（前端/后端/科目选项依赖）
2. 输出科目对照查询、warnings、借贷差值与余额差额相关监控关注点
3. 输出跳转到科目余额表页面兼容性注意项
4. 输出回滚与异常排查建议
5. 对 BUS 未明确项显式标记“待确认”
