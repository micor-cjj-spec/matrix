# 前端提示词

在 `EnterpriseTaxView.vue` 中：
- 为每个 `mappingGap` 生成治理建议。
- 缺口卡片展示建议文案和可选推荐项目编码。
- 点击维护映射时，把 `recommendedItemCode`、`recommendationReason` 带到 `ReportAccountMapView`。
- 增加“开始治理”入口，默认打开第一条缺口。

在 `ReportAccountMapView.vue` 中：
- 读取推荐项目编码和建议依据。
- 缺口治理上下文展示来源建议。
- `inferRecommendedReportItemId` 优先使用路由推荐项目编码。
- 新增弹窗展示推荐依据，且不破坏保存后自动回跳复核。
