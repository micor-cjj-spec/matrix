# API 交付

## 启用下一期间

`POST /period-rollover/from-close-execution/{executionId}`

请求：

```json
{
  "operator": "WEB",
  "remark": "月结工作台启用下一期间"
}
```

返回：

```json
{
  "rollover": {},
  "closeExecution": {},
  "nextPeriod": {},
  "orgFinanceConfig": {}
}
```

## 查询滚动记录

`GET /period-rollover/list?page=1&size=10&forg=1&fromPeriod=2026-04`

可选参数：

- `forg`
- `fromPeriod`
- `toPeriod`
- `closeExecutionId`
- `rolloverStatus`
