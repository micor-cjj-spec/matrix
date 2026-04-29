# 草稿笔记：报表映射缺口治理建议

## 现状观察
- `mappingGaps` 已包含 `reportType`、`templateId`、`accountCode`、`accountName`、`mappingType`、`targetRoute`。
- `ReportAccountMapView` 已支持 `accountCode`、`templateId`、`reportType`、`mode=resolve` 自动定位和打开弹窗。
- 映射页已有 `inferRecommendedReportItemId`，但来源页没有把推荐依据传过去。

## 设计判断
- 本轮先在前端基于已有字段生成治理建议，降低后端变更和部署风险。
- 推荐规则只做“低风险提示”：收入/成本费用/现金/资产/负债权益这些高可解释规则可以自动带入；不确定时只提示诊断，不强行推荐。
- 当前线上可复测的 `1001 - 库存现金` 出现在利润表缺口中，应提示“更像资产负债表科目，先核实口径”，不应盲目推荐利润表项目。

## 验证重点
- 企业纳税表缺口卡片展示治理建议。
- 点击“维护映射”后映射页展示来源建议依据。
- 若存在推荐项目编码，弹窗可自动预填推荐报表项目。
