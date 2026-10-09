# 关账执行中心草稿输出

## 功能范围

1. 批次执行关账接口。
2. 执行前复检并阻止不满足条件的批次。
3. 关闭会计期间状态。
4. 记录关账执行日志。
5. 前端工作台增加“执行关账”按钮与执行记录列表。

## 状态流转

`DRAFT -> SUBMITTED -> APPROVED -> CLOSED`

其中 `CLOSED` 表示该检查批次已完成正式关账执行。

## 交付物

- BUS 文档：`docs/bus/fi/gl/007-period-end-processing/009-month-end-close-execution`
- Prompt 文档：`docs/prompt/fi/gl/007-period-end-processing/009-month-end-close-execution`
- SQL：新增关账执行记录表。
- 后端：执行接口、执行日志实体与查询接口。
- 前端：执行按钮、执行记录列表、状态标签。
