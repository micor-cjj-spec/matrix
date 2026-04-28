# SQL 提示词

新增表 `bizfi_fi_month_end_close_execution`，用于保存正式关账执行记录。

关键索引：

- `fexecution_no` 唯一。
- `fbatch_id` 普通索引。
- `forg + fperiod` 普通索引。

不新增批次表字段，复用 `fapplication_status` 的 `CLOSED` 状态。
