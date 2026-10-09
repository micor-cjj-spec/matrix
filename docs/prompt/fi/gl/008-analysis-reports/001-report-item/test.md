# 报表项目测试提示词

## 目标
根据 `docs/bus/fi/gl/008-analysis-reports/001-report-item/` 下业务文档，以及当前前后端已确认实现，输出报表项目场景的测试与验证提示词。

## 输入上下文
请结合以下文档：
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`
- `docs/prompt/fi/gl/008-analysis-reports/001-report-item/dictionary.md`
- `docs/prompt/fi/gl/008-analysis-reports/001-report-item/api.md`
- `docs/prompt/fi/gl/008-analysis-reports/001-report-item/backend.md`
- `docs/prompt/fi/gl/008-analysis-reports/001-report-item/frontend.md`
- `docs/prompt/fi/gl/008-analysis-reports/001-report-item/sql.md`

## 输出要求
1. 覆盖初始化自动查询与查询场景
2. 覆盖报表模板、项目编码、项目名称过滤场景
3. 覆盖列表展示、分页展示、刷新场景
4. 覆盖 tree/detail/写接口存在但前端未接入的验证边界
5. 对 BUS 未明确项显式标记“待确认”
