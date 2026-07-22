# BOTP 单据转换 SQL 开发提示词

为 BOTP 平台提供 `matrix_botp` 数据库交付物，所有对象遵守 Matrix 数据库命名规范：

- 数据库：`matrix_botp`。
- 表：`matrix_botp_{business_name}`。
- 主键：`fid`。
- 所有字段以 `f` 开头。
- 通用字段包含 `ftenant_id`、`fcreate_by`、`fcreate_time`、`fmodify_by`、`fmodify_time`、`fdelete_flag`、`fversion`。

首版表：

1. document_type：单据类型。
2. document_field：字段元数据。
3. conversion_rule：规则主表。
4. rule_version：不可变版本快照。
5. field_mapping：单头、分录和反写映射。
6. execution：执行任务和幂等键。
7. execution_target：目标创建结果。
8. document_relation：单头关系。
9. document_relation_entry：分录级数量金额关系。
10. writeback_task：反写任务。
11. outbox_event：异步事件。

必须建立请求幂等唯一索引、规则版本唯一索引、源单和目标单关系查询索引。JSON 配置字段使用 JSON 类型，并保留可检索的稳定编码列。
