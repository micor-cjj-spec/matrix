# 月结工作台前端提示词

## BUS 来源

`docs/bus/fi/gl/007-period-end-processing/007-month-end-close-workbench/`

## 生成目标

在 `matrix-web` 中新增财务月结工作台页面，并接入总账期末处理菜单和路由。

## 输入上下文

- 现有期末处理页面位于 `src/views/login/ledger/period-process/`
- 现有接口封装位于 `src/api/periodProcess.js`
- 业务单元加载、期间格式、状态颜色可复用 `periodProcessShared.js`

## 生成约束

1. 页面路径：`src/views/login/ledger/period-process/MonthEndCloseWorkbenchView.vue`
2. 路由路径：`/ledger/month-end-close-workbench`
3. 页面首版只读，不提供正式关账按钮。
4. 必须展示查询区、准备度概览、检查项表格、月结步骤表格、预警提示。
5. 下钻按钮根据后端返回的 `routePath` 跳转。

## 验收标准

- 能选择业务单元和期间查询。
- 能展示 `closeStatus`、`readinessScore`、`blockingCount`、`pendingVoucherCount`。
- 检查项状态可读，阻塞项醒目。
- 总账首页期末处理分组能进入页面。

