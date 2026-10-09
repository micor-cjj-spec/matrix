# 业务规则

## 查询规则
- `period` 必须是 `yyyy-MM`，无效时返回 warning。
- 过滤条件都作用于已识别完成的现金相关凭证记录，而不是直接作用于原始总账分录。
- `accountCode` 为关键字匹配，同时检查现金科目和对方科目字符串。
- `keyword` 匹配凭证号、摘要、现金流项目编码/名称。

## 统计规则
- `cashVoucherCount` 是过滤后记录数。
- `directCount / heuristicCount / unknownCount / mixedCount / transferCount` 都是过滤后统计。
- `cashInAmount / cashOutAmount / netAmount` 也是过滤后金额统计。

## 说明规则
- `reason` 字段用于解释当前凭证为什么被归到某种识别方式。
- `UNKNOWN_ITEM` 和 `MIXED_ITEM` 不应直接进入主表最终口径，需优先复核。
