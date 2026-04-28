# API 提示词

## 新增接口

- `POST /month-end-check-batch/{fid}/execute-close`
- `GET /month-end-close-execution/list`

## 返回结构

执行接口返回 `MonthEndCloseExecutionResultVO`：

- `execution`
- `batch`
- `accountingPeriod`
- `workbench`

## 错误

- 批次不存在。
- 批次未批准。
- 批次已关账。
- 实时检查未通过。
- 会计期间不存在。
- 会计期间不是 `OPEN`。
