# 月结检查批次接口交付

## 生成批次

`POST /month-end-check-batch`

请求体：

```json
{
  "forg": 1,
  "period": "2026-04",
  "createdBy": "WEB",
  "remark": "月结工作台生成检查批次"
}
```

## 查询列表

`GET /month-end-check-batch/list?page=1&size=10&forg=1&period=2026-04`

返回 `IPage<BizfiFiMonthEndCheckBatch>`。

## 查询详情

`GET /month-end-check-batch/{fid}`

## 提交申请

`POST /month-end-check-batch/{fid}/submit`

## 批准申请

`POST /month-end-check-batch/{fid}/approve`

## 状态限制

- DRAFT 且无阻塞项才允许提交。
- SUBMITTED 才允许批准。

