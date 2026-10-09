# 月结检查批次接口提示词

## 接口清单

- `POST /month-end-check-batch`
- `GET /month-end-check-batch/list`
- `GET /month-end-check-batch/{fid}`
- `POST /month-end-check-batch/{fid}/submit`
- `POST /month-end-check-batch/{fid}/approve`

## 返回

统一返回 `ApiResponse`。

列表接口返回 `IPage<BizfiFiMonthEndCheckBatch>`。

