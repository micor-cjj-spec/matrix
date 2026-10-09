# 开放平台 SQL 开发提示词

## 目标

为 Matrix OpenAPI 第一阶段提供可重复执行的 MySQL 表结构和三条凭证只读 API 种子数据。

## 要求

1. 表名以 `matrix_open_api_` 开头。
2. AppKey、API 编码、请求 ID 必须有唯一索引。
3. 授权表必须保证应用和 API 组合唯一。
4. 调用日志按应用/API/请求时间建立查询索引。
5. AppSecret 只保存密文。
6. 种子 API 默认 `PUBLISHED`，权限范围为 `fi.voucher.read`。
