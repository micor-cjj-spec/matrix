# 接口说明

## 查询接口
- `GET /arap-doc/list`
- `GET /arap-doc/{fid}`

## 写接口
- `POST /arap-doc`
- `PUT /arap-doc`
- `DELETE /arap-doc/{fid}`

## 流转接口
- `POST /arap-doc/submit/{fid}`
- `POST /arap-doc/submit/by-number`
- `POST /arap-doc/audit/{fid}`
- `POST /arap-doc/audit/by-number`
- `POST /arap-doc/reject/{fid}`
- `POST /arap-doc/reject/by-number`
- `POST /arap-doc/voucher/{fid}`
- `POST /arap-doc/voucher/by-number`
- `GET /arap-doc/by-voucher`

## 主要参数
- 查询时 `docType=AR`

## 前端关联
- 前端 API：`arap-doc.js`
- 前端页面：`ArapDocView.vue`
- 路由：`/receivable/manage`
