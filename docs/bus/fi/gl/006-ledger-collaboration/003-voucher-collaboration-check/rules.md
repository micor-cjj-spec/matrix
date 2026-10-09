# 业务规则

## 查询规则
- 日期格式必须为 `yyyy-MM-dd`。
- `startDate` 为空默认当月第一天，`endDate` 为空默认当天。
- `issueCode`、`severity`、`status` 传 `ALL` 时不过滤。
- `onlyIssue=true` 时只返回异常记录。

## 问题类型
当前检查输出的问题编码包括：
- `MISSING_LINES`
- `LINE_NOT_BALANCED`
- `HEADER_LINE_AMOUNT`
- `DUPLICATE_NUMBER`
- `MISSING_GL_ENTRY`
- `GL_LINE_COUNT_DIFF`
- `GL_LINE_MISMATCH`
- `PREMATURE_GL_ENTRY`

## 等级口径
- 关键链路问题通常标为 `HIGH`
- 一般金额或条数差异通常标为 `MEDIUM`
- 健康记录固定为 `LOW`
