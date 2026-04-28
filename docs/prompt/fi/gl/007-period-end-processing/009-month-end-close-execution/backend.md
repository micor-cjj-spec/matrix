# 后端提示词

## 目标

实现月结检查批次的正式关账执行能力。

## 要求

1. 新增关账执行记录实体、Mapper、Service、Controller。
2. 在 `BizfiFiMonthEndCheckBatchService` 增加 `executeClose`。
3. 仅允许 `APPROVED` 批次执行。
4. 执行前调用 `monthEndWorkbench` 进行实时复检。
5. 仅当实时 `canClose=true` 且会计期间为 `OPEN` 时关闭期间。
6. 使用现有 `BizfiFiAccountingPeriodService.close` 关闭期间。
7. 成功后写入执行记录，并将批次状态置为 `CLOSED`。
8. 返回执行记录、批次、会计期间和实时检查快照。
