# 后端开发提示词

本轮不新增后端接口，不改数据库结构。后端仅需保持既有接口兼容：

- `POST /report-account-map` 可以继续创建报表科目映射。
- `GET /analysis-report/enterprise-tax` 继续返回 `mappingGaps`。
- `ReportMappingGapVO.targetRoute` 保持可用。

如后续需要更强推荐能力，再考虑在 `mappingGaps` 中增加推荐报表项目字段。
