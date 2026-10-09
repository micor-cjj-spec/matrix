# 往来核销方案运维提示词

## 目标
根据 `docs/bus/fi/gl/004-counterparty-management/001-counterparty-writeoff-plan/` 下业务文档，以及当前已确认实现，输出往来核销方案场景的发布、运维、监控与问题处理提示词。

## 输入上下文
请结合以下文档：
- `flow.md`
- `rules.md`
- `api.md`
- `docs/prompt/fi/gl/004-counterparty-management/001-counterparty-writeoff-plan/backend.md`
- `docs/prompt/fi/gl/004-counterparty-management/001-counterparty-writeoff-plan/frontend.md`
- `docs/prompt/fi/gl/004-counterparty-management/001-counterparty-writeoff-plan/sql.md`
- `docs/prompt/fi/gl/004-counterparty-management/001-counterparty-writeoff-plan/test.md`
- `docs/prompt/fi/gl/004-counterparty-management/001-counterparty-writeoff-plan/review.md`

## 输出要求
1. 输出发布依赖检查项（前端/后端/往来方依赖）
2. 输出方案查询、warnings、统计字段相关监控关注点
3. 输出“去自动核销”跳转参数兼容性注意项
4. 输出回滚与异常排查建议
5. 对 BUS 未明确项显式标记“待确认”
