# 月结检查批次后端提示词

## BUS 来源

`docs/bus/fi/gl/007-period-end-processing/008-month-end-check-batch/`

## 生成目标

在 `fi-service` 中新增月结检查批次保存、列表查询、详情查询、提交申请和批准申请能力。

## 实现要求

1. 新增实体 `BizfiFiMonthEndCheckBatch`。
2. 新增 Mapper、Service、ServiceImpl、Controller。
3. 创建批次时调用 `BizfiFiPeriodProcessService.monthEndWorkbench` 重新执行检查。
4. 使用 Jackson 将 `MonthEndWorkbenchResultVO` 保存为 `fsnapshotJson`。
5. `fblockingCount > 0` 的批次禁止提交。
6. 批准申请只更新申请状态，不修改会计期间。

## 接口

- `POST /month-end-check-batch`
- `GET /month-end-check-batch/list`
- `GET /month-end-check-batch/{fid}`
- `POST /month-end-check-batch/{fid}/submit`
- `POST /month-end-check-batch/{fid}/approve`

