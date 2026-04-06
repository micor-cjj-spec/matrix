# 报表项目接口提示词

## 目标
根据 `docs/bus/fi/gl/008-analysis-reports/001-report-item/` 下业务文档，输出报表项目场景的接口契约提示词，为前后端联调、测试与评审提供统一接口口径。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`
- `docs/prompt/fi/gl/008-analysis-reports/001-report-item/dictionary.md`

同时结合当前已确认实现：
- `GET /report-item/list`
- `GET /report-item/tree`
- `GET /report-item/{fid}`
- `POST /report-item`
- `PUT /report-item`
- `DELETE /report-item/{fid}`
- 前端 API：`reportItemApi`、`reportTemplateApi`
- 页面：`ReportItemView.vue`

## 输出要求
1. 输出查询、树查询、详情、写接口契约
2. 输出查询参数、分页返回、树结构返回、详情返回的结构说明
3. 说明当前页面已接入与未接入的接口范围
4. 说明模板加载与项目列表查询的关联关系
5. 对 BUS 未明确项显式标记“待确认”
