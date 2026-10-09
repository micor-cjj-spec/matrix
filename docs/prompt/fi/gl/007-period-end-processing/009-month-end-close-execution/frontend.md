# 前端提示词

## 目标

在月结工作台补充关账执行入口和执行记录。

## 要求

1. `src/api/periodProcess.js` 增加执行关账和执行记录查询接口。
2. `periodProcessShared.js` 增加 `CLOSED` 批次状态和执行状态映射。
3. `MonthEndCloseWorkbenchView.vue` 在已批准批次行展示“执行关账”。
4. 点击执行前进行确认。
5. 执行成功后刷新工作台、批次列表、执行记录。
6. 页面新增关账执行记录表格。
