# 往来多维分析表运维提示词

## 目标
根据 `docs/bus/fi/gl/004-counterparty-management/007-counterparty-multi-analysis/` 下业务文档，以及当前已确认实现，输出往来多维分析表场景的发布、运维、监控与问题处理提示词。

## 输入上下文
请结合以下文档：
- `flow.md`
- `rules.md`
- `api.md`
- `docs/prompt/fi/gl/004-counterparty-management/007-counterparty-multi-analysis/backend.md`
- `docs/prompt/fi/gl/004-counterparty-management/007-counterparty-multi-analysis/frontend.md`
- `docs/prompt/fi/gl/004-counterparty-management/007-counterparty-multi-analysis/sql.md`
- `docs/prompt/fi/gl/004-counterparty-management/007-counterparty-multi-analysis/test.md`
- `docs/prompt/fi/gl/004-counterparty-management/007-counterparty-multi-analysis/review.md`

## 输出要求
1. 输出发布依赖检查项（前端/后端/分组维度依赖）
2. 输出多维分析查询、warnings、groupDimension 相关监控关注点
3. 输出只读分析边界兼容性注意项
4. 输出回滚与异常排查建议
5. 对 BUS 未明确项显式标记“待确认”
