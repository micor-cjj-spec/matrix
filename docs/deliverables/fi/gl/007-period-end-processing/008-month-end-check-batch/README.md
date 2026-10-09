# 月结检查批次交付说明

## 场景

月结检查批次留痕与关账申请底座。

## 追溯链路

- Draft：`docs/draft/2026/04/28/002-month-end-check-batch`
- BUS：`docs/bus/fi/gl/007-period-end-processing/008-month-end-check-batch`
- Prompt：`docs/prompt/fi/gl/007-period-end-processing/008-month-end-check-batch`

## 代码落点

后端：

- `BizfiFiMonthEndCheckBatchController`
- `BizfiFiMonthEndCheckBatchService`
- `BizfiFiMonthEndCheckBatchServiceImpl`
- `BizfiFiMonthEndCheckBatch`
- `BizfiFiMonthEndCheckBatchMapper`

前端：

- `MonthEndCloseWorkbenchView.vue`
- `periodProcess.js`
- `periodProcessShared.js`

SQL：

- `sql/bizfi_fi_month_end_check_batch_v1.sql`

## 首版能力

- 生成检查批次
- 保存月结工作台检查快照
- 查询历史批次
- DRAFT 批次提交申请
- SUBMITTED 批次批准
- 批准不修改会计期间状态

