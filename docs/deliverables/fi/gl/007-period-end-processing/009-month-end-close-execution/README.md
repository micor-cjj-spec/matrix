# 关账执行中心交付说明

## 范围

本次交付将月结检查批次推进到正式关账动作：

- 已批准批次执行关账。
- 执行前实时复检。
- 关闭会计期间。
- 写入关账执行记录。
- 前端展示执行按钮与执行流水。

## 文档

- Draft：`docs/draft/2026/04/28/003-month-end-close-execution`
- BUS：`docs/bus/fi/gl/007-period-end-processing/009-month-end-close-execution`
- Prompt：`docs/prompt/fi/gl/007-period-end-processing/009-month-end-close-execution`

## 后端

- `BizfiFiMonthEndCloseExecution`
- `BizfiFiMonthEndCloseExecutionMapper`
- `BizfiFiMonthEndCloseExecutionService`
- `BizfiFiMonthEndCloseExecutionServiceImpl`
- `BizfiFiMonthEndCloseExecutionController`
- `MonthEndCloseExecuteRequestVO`
- `MonthEndCloseExecutionResultVO`
- `BizfiFiMonthEndCheckBatchService.executeClose`

## 前端

- `src/api/periodProcess.js`
- `src/views/login/ledger/period-process/periodProcessShared.js`
- `src/views/login/ledger/period-process/MonthEndCloseWorkbenchView.vue`

## SQL

- `sql/bizfi_fi_month_end_close_execution_v1.sql`
