# 后端提示词

本轮不修改后端运行时代码。保留现有企业纳税表 `mappingGaps` 响应结构，由前端基于已有字段生成治理建议。

如未来需要后端统一推荐口径，可扩展 `ReportMappingGapVO`，增加 `suggestedItemCode`、`suggestedItemName`、`suggestionReason` 字段，并由利润表/资产负债表服务共同生成。
