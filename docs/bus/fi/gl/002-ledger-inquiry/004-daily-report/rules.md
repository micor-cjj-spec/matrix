# 业务规则

## 查询规则
- 日期格式必须为 `yyyy-MM-dd`。
- 未传 `endDate` 时默认当天；未传 `startDate` 时默认 `endDate` 所在月第一天。
- `startDate` 不能晚于 `endDate`。
- `accountCode` 使用前缀匹配（`likeRight`）。

## 统计规则
- 当前仅基于已过账总账分录统计。
- `voucherCount` 按当日唯一凭证ID去重统计。
- 汇总区的 `voucherCount` 也是期间内唯一凭证ID去重统计。

## 提示规则
- 继承基础提示：当前尚未按业务单元隔离，且尚未切换为期间余额表。
