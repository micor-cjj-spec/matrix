# 利润表运维提示词

## 目标
根据 `docs/bus/fi/gl/008-analysis-reports/004-profit-statement/` 下业务文档，以及当前已确认前端接入事实，输出利润表场景的发布、运维、监控与问题处理提示词。

## 输入上下文
请结合以下文档：
- `flow.md`
- `rules.md`
- `api.md`
- `docs/prompt/fi/gl/008-analysis-reports/004-profit-statement/backend.md`
- `docs/prompt/fi/gl/008-analysis-reports/004-profit-statement/frontend.md`
- `docs/prompt/fi/gl/008-analysis-reports/004-profit-statement/sql.md`
- `docs/prompt/fi/gl/008-analysis-reports/004-profit-statement/test.md`
- `docs/prompt/fi/gl/008-analysis-reports/004-profit-statement/review.md`

## 输出要求
1. 输出发布依赖检查项（前端/接口/模板依赖）
2. 输出主表查询、warnings、checks 相关监控关注点
3. 输出 showZero 展示兼容性注意项
4. 明确标注“后端内部实现文件本轮未定位”
5. 对 BUS 未明确项显式标记“待确认”
