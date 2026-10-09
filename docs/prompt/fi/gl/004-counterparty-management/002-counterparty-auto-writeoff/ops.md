# 往来自动核销运维提示词

## 目标
根据 `docs/bus/fi/gl/004-counterparty-management/002-counterparty-auto-writeoff/` 下业务文档，以及当前已确认实现，输出往来自动核销场景的发布、运维、监控与问题处理提示词。

## 输入上下文
请结合以下文档：
- `flow.md`
- `rules.md`
- `api.md`
- `docs/prompt/fi/gl/004-counterparty-management/002-counterparty-auto-writeoff/backend.md`
- `docs/prompt/fi/gl/004-counterparty-management/002-counterparty-auto-writeoff/frontend.md`
- `docs/prompt/fi/gl/004-counterparty-management/002-counterparty-auto-writeoff/sql.md`
- `docs/prompt/fi/gl/004-counterparty-management/002-counterparty-auto-writeoff/test.md`
- `docs/prompt/fi/gl/004-counterparty-management/002-counterparty-auto-writeoff/review.md`

## 输出要求
1. 输出发布依赖检查项（前端/后端/往来方与操作人依赖）
2. 输出自动核销执行、warnings、成功提示区相关监控关注点
3. 输出从方案页带入参数与执行接口兼容性注意项
4. 输出回滚与异常排查建议
5. 对 BUS 未明确项显式标记“待确认”
