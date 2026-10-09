# 月结检查批次前端提示词

## BUS 来源

`docs/bus/fi/gl/007-period-end-processing/008-month-end-check-batch/`

## 生成目标

在月结工作台页面新增检查批次留痕和关账申请底座操作。

## 实现要求

1. 在 `src/api/periodProcess.js` 增加批次相关接口封装。
2. 在 `MonthEndCloseWorkbenchView.vue` 增加“生成检查批次”和“刷新历史批次”按钮。
3. 增加历史批次表格。
4. DRAFT 且阻塞项为 0 时允许提交。
5. SUBMITTED 时允许批准。
6. 操作完成后刷新工作台和批次列表。

