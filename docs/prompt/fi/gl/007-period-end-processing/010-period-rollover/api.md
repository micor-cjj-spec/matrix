# API 提示词

## 新增接口

- `POST /period-rollover/from-close-execution/{executionId}`
- `GET /period-rollover/list`

## 错误

- 关账执行记录不存在。
- 关账执行记录未成功。
- 关账执行记录不是关闭结果。
- 执行记录已滚动。
- 组织财务参数不存在或未启用。
- 当前期间已不等于被关账期间。
- 下一期间已关闭。
