# 报表项目运维提示词

## 目标
根据 `docs/bus/fi/gl/008-analysis-reports/001-report-item/` 下业务文档，以及当前已确认实现，输出报表项目场景的发布、运维、监控与问题处理提示词。

## 输入上下文
请结合以下文档：
- `flow.md`
- `rules.md`
- `api.md`
- `docs/prompt/fi/gl/008-analysis-reports/001-report-item/backend.md`
- `docs/prompt/fi/gl/008-analysis-reports/001-report-item/frontend.md`
- `docs/prompt/fi/gl/008-analysis-reports/001-report-item/sql.md`
- `docs/prompt/fi/gl/008-analysis-reports/001-report-item/test.md`
- `docs/prompt/fi/gl/008-analysis-reports/001-report-item/review.md`

## 输出要求
1. 输出发布依赖检查项（前端/后端/模板依赖）
2. 输出 list/tree/detail/写接口相关监控关注点
3. 输出前端未接入写入口时的兼容性注意项
4. 输出回滚与异常排查建议
5. 对 BUS 未明确项显式标记“待确认”
