# 前端提示词

## 目标

在月结工作台增加启用下一期间能力。

## 要求

1. `src/api/periodProcess.js` 增加期间滚动接口。
2. `periodProcessShared.js` 增加滚动状态映射。
3. `MonthEndCloseWorkbenchView.vue` 的关账执行记录行增加“启用下一期间”按钮。
4. 已有滚动记录的执行记录不再展示按钮。
5. 页面新增期间滚动记录表格。
6. 滚动成功后刷新工作台、批次、执行记录、滚动记录。
