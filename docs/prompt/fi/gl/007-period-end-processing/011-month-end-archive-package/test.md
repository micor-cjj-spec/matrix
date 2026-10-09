# 测试提示词

## 后端

1. 无任何记录返回 `NOT_STARTED`。
2. 有检查批次返回 `CHECKED` 或 `BLOCKED`。
3. 已批准未关账返回 `APPROVED_PENDING_CLOSE`。
4. 已关账返回 `CLOSED`。
5. 已滚动返回 `ROLLED`。

## 前端

1. 页面展示归档状态和结论。
2. 里程碑按已有记录展示。
3. 刷新时同步刷新归档包。
