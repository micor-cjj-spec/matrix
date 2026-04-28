# 后端提示词

## 目标

实现月结归档包聚合查询接口。

## 要求

1. 新增 `BizfiFiMonthEndArchiveService` 与实现类。
2. 新增 `BizfiFiMonthEndArchiveController`。
3. 新增 `MonthEndArchivePackageVO` 和 `MonthEndArchiveMilestoneVO`。
4. 接口路径：`GET /month-end-archive/package`。
5. 聚合最新检查批次、关账执行记录、期间滚动记录。
6. 调用 `BizfiFiPeriodProcessService.monthEndWorkbench` 补充实时指标。
7. 输出归档状态、结论、里程碑、风险提示。
