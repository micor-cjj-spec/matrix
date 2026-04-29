# 前端提示词：映射缺口展示与跳转

请在 `matrix-web` 前端实现：

1. 企业纳税表读取 `mappingGaps`。
2. 当缺口非空时展示待处理区域。
3. 点击维护按钮跳转 `/ledger/report-account-map` 并带上 `accountCode`、`reportType`。
4. 报表科目映射页读取 `accountCode` 查询参数并自动筛选会计科目。
