# 报表科目映射缺口治理分析记录

## 现状依据
- 利润表后端：`BizfiFiProfitStatementServiceImpl`
- 资产负债表后端：`BizfiFiBalanceSheetServiceImpl`
- 企业纳税表后端：`BizfiFiAnalysisReportServiceImpl.enterpriseTax`
- 报表科目映射页：`ReportAccountMapView.vue`

## 问题
- 未映射科目目前只进入 `warnings` 字符串。
- 企业纳税表复用利润表结果后，只能展示英文告警。
- 用户无法从告警直接跳转到维护页。

## 方案
- 新增 `ReportMappingGapVO`，承载报表类型、模板、科目、建议动作和目标路由。
- `ReportQueryResultVO` 增加 `mappingGaps`。
- 企业纳税表结果透传利润表 `mappingGaps`。
- 企业纳税表页面展示缺口待办，并跳转报表科目映射页。
- 报表科目映射页读取 `accountCode` 查询参数，自动筛选会计科目。
