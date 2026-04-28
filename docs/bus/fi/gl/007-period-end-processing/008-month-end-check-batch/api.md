# 接口说明

## 1. 生成批次

`POST /month-end-check-batch`

请求体：

- `forg`
- `period`
- `createdBy`
- `remark`

## 2. 查询批次列表

`GET /month-end-check-batch/list`

参数：

- `page`
- `size`
- `forg`
- `period`
- `applicationStatus`

## 3. 查询批次详情

`GET /month-end-check-batch/{fid}`

## 4. 提交申请

`POST /month-end-check-batch/{fid}/submit`

## 5. 批准申请

`POST /month-end-check-batch/{fid}/approve`

