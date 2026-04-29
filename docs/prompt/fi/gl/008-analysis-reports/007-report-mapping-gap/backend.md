# 后端提示词：报表科目映射缺口治理

请在 `matrix` 后端实现结构化映射缺口：

1. 新增 `ReportMappingGapVO`。
2. `ReportQueryResultVO` 增加 `mappingGaps`。
3. 利润表、资产负债表聚合时将未映射科目写入 `mappingGaps`。
4. 企业纳税表透传利润表 `mappingGaps`。
5. 保留原有告警兼容，但中文化关键映射缺口提示。
