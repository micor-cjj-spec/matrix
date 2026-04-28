# SQL 提示词

新增表 `bizfi_fi_period_rollover`。

关键索引：

- `frollover_no` 唯一。
- `fclose_execution_id` 唯一，防止重复滚动。
- `forg + ffrom_period` 普通索引。
