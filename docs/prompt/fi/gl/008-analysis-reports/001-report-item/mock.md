# 报表项目样例提示词

## 目标
根据 `docs/bus/fi/gl/008-analysis-reports/001-report-item/` 下业务文档，以及当前已确认实现，输出报表项目场景的样例数据与 mock 提示词。

## 输入上下文
请结合以下文档：
- `fields.md`
- `page.md`
- `api.md`
- `docs/prompt/fi/gl/008-analysis-reports/001-report-item/dictionary.md`
- `docs/prompt/fi/gl/008-analysis-reports/001-report-item/api.md`
- `docs/prompt/fi/gl/008-analysis-reports/001-report-item/frontend.md`
- `docs/prompt/fi/gl/008-analysis-reports/001-report-item/test.md`

## 输出要求
1. 输出默认查询样例、分页样例、列表样例、树样例、详情样例
2. 输出不同模板过滤、关键字过滤、空结果等样例
3. 标记当前页面未接入但后端已提供的写接口样例
4. 对 BUS 未明确项显式标记“待确认”
