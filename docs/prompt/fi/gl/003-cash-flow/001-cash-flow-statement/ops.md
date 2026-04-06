# 现金流量表运维提示词

## 目标
根据 `docs/bus/fi/gl/003-cash-flow/001-cash-flow-statement/` 下业务文档，以及当前已确认实现，输出现金流量表场景的发布、运维、监控与问题处理提示词。

## 输入上下文
请结合以下文档：
- `flow.md`
- `rules.md`
- `api.md`
- `docs/prompt/fi/gl/003-cash-flow/001-cash-flow-statement/backend.md`
- `docs/prompt/fi/gl/003-cash-flow/001-cash-flow-statement/frontend.md`
- `docs/prompt/fi/gl/003-cash-flow/001-cash-flow-statement/sql.md`
- `docs/prompt/fi/gl/003-cash-flow/001-cash-flow-statement/test.md`
- `docs/prompt/fi/gl/003-cash-flow/001-cash-flow-statement/review.md`

## 输出要求
1. 输出发布依赖检查项（前端/后端/业务单元与模板依赖）
2. 输出现金流量表查询、checks、warnings 相关监控关注点
3. 输出初始化自动查询与跳转参数兼容性注意项
4. 输出回滚与异常排查建议
5. 对 BUS 未明确项显式标记“待确认”
