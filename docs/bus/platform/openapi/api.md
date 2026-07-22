# 凭证 OpenAPI 契约 V1

## 1. 查询凭证列表

`GET /open-api/v1/fi/vouchers`

参数：`pageNo`、`pageSize`、`voucherNumber`、`status`、`startDate`、`endDate`。

## 2. 查询凭证详情

`GET /open-api/v1/fi/vouchers/{voucherId}`

## 3. 查询凭证分录

`GET /open-api/v1/fi/vouchers/{voucherId}/lines`

## 4. 统一响应

```json
{
  "code": "0",
  "message": "success",
  "requestId": "req_xxx",
  "data": {}
}
```

## 5. 对外凭证字段

- `voucherId`
- `voucherNumber`
- `voucherDate`
- `summary`
- `amount`
- `status`
- `createdBy`
- `createdTime`
- `auditedBy`
- `auditedTime`
- `postedBy`
- `postedTime`

不返回内部删除标识、数据库控制字段和备注中的内部诊断信息。

## 6. 对外分录字段

- `lineId`
- `lineNumber`
- `accountCode`
- `summary`
- `debitAmount`
- `creditAmount`
- `currencyCode`
- `exchangeRate`
- `originalAmount`
- `cashflowItemCode`
