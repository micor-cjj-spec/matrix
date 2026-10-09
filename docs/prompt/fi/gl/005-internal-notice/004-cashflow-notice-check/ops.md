# 现金流通知单勾稽运维提示词

## 目标
根据 `docs/bus/fi/gl/005-internal-notice/004-cashflow-notice-check/` 下业务文档，以及当前已确认实现，输出现金流通知单勾稽场景的发布、运维、监控与问题处理提示词。

## 输入上下文
请结合以下文档：
- `flow.md`
- `rules.md`
- `api.md`
- `docs/prompt/fi/gl/005-internal-notice/004-cashflow-notice-check/backend.md`
- `docs/prompt/fi/gl/005-internal-notice/004-cashflow-notice-check/frontend.md`
- `docs/prompt/fi/gl/005-internal-notice/004-cashflow-notice-check/sql.md`
- `docs/prompt/fi/gl/005-internal-notice/004-cashflow-notice-check/test.md`
- `docs/prompt/fi/gl/005-internal-notice/004-cashflow-notice-check/review.md`

## 输出要求
1. 输出发布依赖检查项（前端/后端/现金流候选结果依赖）
2. 输出勾稽查询、warnings、跳转凭证页相关监控关注点
3. 输出跳转到凭证页兼容性注意项
4. 输出回滚与异常排查建议
5. 对 BUS 未明确项显式标记“待确认”
