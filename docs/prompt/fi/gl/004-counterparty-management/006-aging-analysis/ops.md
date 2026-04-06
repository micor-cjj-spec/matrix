# 账龄分析表运维提示词

## 目标
根据 `docs/bus/fi/gl/004-counterparty-management/006-aging-analysis/` 下业务文档，以及当前已确认实现，输出账龄分析表场景的发布、运维、监控与问题处理提示词。

## 输入上下文
请结合以下文档：
- `flow.md`
- `rules.md`
- `api.md`
- `docs/prompt/fi/gl/004-counterparty-management/006-aging-analysis/backend.md`
- `docs/prompt/fi/gl/004-counterparty-management/006-aging-analysis/frontend.md`
- `docs/prompt/fi/gl/004-counterparty-management/006-aging-analysis/sql.md`
- `docs/prompt/fi/gl/004-counterparty-management/006-aging-analysis/test.md`
- `docs/prompt/fi/gl/004-counterparty-management/006-aging-analysis/review.md`

## 输出要求
1. 输出发布依赖检查项（前端/后端/信用额度主数据依赖）
2. 输出账龄分析查询、warnings、风险状态相关监控关注点
3. 输出只读分析边界兼容性注意项
4. 输出回滚与异常排查建议
5. 对 BUS 未明确项显式标记“待确认”
