# 财务期间滚动交付说明

## 范围

本次交付补齐关账后的下一期间启用能力：

- 基于成功关账执行记录启用下一期间。
- 自动计算下一期间。
- 自动创建或复用下一会计期间。
- 更新组织财务参数当前期间。
- 写入期间滚动记录。
- 前端展示启用按钮和滚动记录。

## 文档

- Draft：`docs/draft/2026/04/29/004-period-rollover`
- BUS：`docs/bus/fi/gl/007-period-end-processing/010-period-rollover`
- Prompt：`docs/prompt/fi/gl/007-period-end-processing/010-period-rollover`

## 后端

- `BizfiFiPeriodRollover`
- `BizfiFiPeriodRolloverMapper`
- `BizfiFiPeriodRolloverService`
- `BizfiFiPeriodRolloverServiceImpl`
- `BizfiFiPeriodRolloverController`
- `PeriodRolloverRequestVO`
- `PeriodRolloverResultVO`

## 前端

- `src/api/periodProcess.js`
- `src/views/login/ledger/period-process/periodProcessShared.js`
- `src/views/login/ledger/period-process/MonthEndCloseWorkbenchView.vue`

## SQL

- `sql/bizfi_fi_period_rollover_v1.sql`
