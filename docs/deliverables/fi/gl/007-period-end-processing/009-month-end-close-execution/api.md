# API 交付

## 执行关账

`POST /month-end-check-batch/{fid}/execute-close`

请求：

```json
{
  "operator": "WEB",
  "remark": "月结工作台执行关账"
}
```

返回：

```json
{
  "execution": {},
  "batch": {},
  "accountingPeriod": {},
  "workbench": {}
}
```

## 查询执行记录

`GET /month-end-close-execution/list?page=1&size=10&forg=1&period=2026-04`

可选参数：

- `forg`
- `period`
- `batchId`
- `executionStatus`
