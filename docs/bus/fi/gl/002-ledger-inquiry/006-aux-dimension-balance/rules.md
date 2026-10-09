# 业务规则

## 查询规则
- 日期格式必须为 `yyyy-MM-dd`。
- 未传 `endDate` 时默认当天；未传 `startDate` 时默认 `endDate` 所在月第一天。
- `startDate` 不能晚于 `endDate`。
- `accountCode` 使用前缀匹配（`likeRight`）。
- `dimensionCode` 精确匹配现金流项目编码。

## 统计规则
- 当前仅支持按现金流项目 + 科目组合进行辅助余额统计。
- 每个组合的余额方向由组合净额推导。
- 维度相关提示和基础提示会一并返回。
