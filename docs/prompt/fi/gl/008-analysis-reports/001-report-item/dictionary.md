# 报表项目数据字典提示词

## 目标
根据 `docs/bus/fi/gl/008-analysis-reports/001-report-item/` 下业务文档，输出报表项目场景的数据字典、层级口径、模板口径、展示口径与维护边界，作为接口、前后端、测试的统一语义基础。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`

同时结合当前已确认实现：
- 前端页面：`ReportItemView.vue`
- 前端 API：`reportItemApi`、`reportTemplateApi`
- 后端：`BizfiFiReportItemController / ServiceImpl`
- 接口：`/report-item/*`

## 输出要求
1. 输出查询条件字段字典
2. 输出列表区字段字典
3. 输出模板、项目编码、项目名称、行次、层级、行类型、期间模式、可下钻、排序等口径
4. 说明分页与树形口径的区别
5. 对未在页面开放的维护能力标记“后端已提供 / 前端未接入”
