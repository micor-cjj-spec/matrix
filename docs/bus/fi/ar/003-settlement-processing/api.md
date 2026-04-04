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
- `POST /arap-doc/audit/{fid}`
- `POST /arap-doc/reject/{fid}`
- `POST /arap-doc/voucher/{fid}`

## 主要参数
- 查询时 `docType=AR_SETTLEMENT`

## 前端关联
- 前端 API：`arap-doc.js`
- 前端页面：`ArapDocView.vue`
- 路由：`/receivable/settlement`
