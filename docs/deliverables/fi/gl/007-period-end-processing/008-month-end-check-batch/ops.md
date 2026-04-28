# 月结检查批次运维交付

## 上线前置

需要执行：

`sql/bizfi_fi_month_end_check_batch_v1.sql`

## 运维关注

- `fsnapshot_json` 为 LONGTEXT，批次数量增加后需关注表容量。
- 列表接口默认按生成时间倒序。
- 批准状态不代表期间关闭，正式关账仍需后续功能。

