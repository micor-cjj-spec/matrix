# 月结归档包交付说明

## 范围

本次交付提供月结归档包 / 关账报告中心第一版：

- 按组织和期间查询月结归档包。
- 聚合最新检查批次、关账执行记录、期间滚动记录。
- 聚合实时月结工作台指标。
- 输出归档状态、归档结论、里程碑和风险提示。
- 前端月结工作台新增归档包展示区域。

## 文档

- Draft：`docs/draft/2026/04/29/005-month-end-archive-package`
- BUS：`docs/bus/fi/gl/007-period-end-processing/011-month-end-archive-package`
- Prompt：`docs/prompt/fi/gl/007-period-end-processing/011-month-end-archive-package`

## 后端

- `BizfiFiMonthEndArchiveController`
- `BizfiFiMonthEndArchiveService`
- `BizfiFiMonthEndArchiveServiceImpl`
- `MonthEndArchivePackageVO`
- `MonthEndArchiveMilestoneVO`

## 前端

- `src/api/periodProcess.js`
- `src/views/login/ledger/period-process/periodProcessShared.js`
- `src/views/login/ledger/period-process/MonthEndCloseWorkbenchView.vue`

## SQL

第一版不新增 SQL。
