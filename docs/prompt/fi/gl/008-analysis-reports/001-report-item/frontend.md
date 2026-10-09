# 报表项目前端提示词

## 目标
结合 `docs/bus/fi/gl/008-analysis-reports/001-report-item/` 下业务文档，以及当前前端已确认实现，输出报表项目场景的前端实现建议与补齐提示词。

## 输入上下文
请结合以下文档：
- `page.md`
- `fields.md`
- `flow.md`
- `api.md`
- `docs/prompt/fi/gl/008-analysis-reports/001-report-item/dictionary.md`
- `docs/prompt/fi/gl/008-analysis-reports/001-report-item/api.md`
- `docs/prompt/fi/gl/008-analysis-reports/001-report-item/backend.md`

同时结合当前已确认实现：
- 页面：`ReportItemView.vue`
- 查询区：报表模板、项目编码、项目名称
- 工具按钮：刷新
- 列表区：模板、项目编码、项目名称、行次、层级、行类型、期间模式、可下钻、排序
- 分页区：分页和总数
- 初始化时加载报表模板并自动查询
- 当前页面未接入新增、编辑、删除入口

## 输出要求
1. 输出查询区、列表区、分页区交互建议
2. 说明初始化自动查询、查询、重置、刷新行为
3. 说明列表字段展示逻辑与分页逻辑
4. 标记后端已提供但前端未接入的维护能力
5. 对 BUS 未明确项显式标记“待确认”
