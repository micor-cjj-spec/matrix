# 接口说明

## 查询接口
- `GET /arap-doc/aging/summary`
- `GET /arap-doc/credit/warnings`
- `GET /arap-doc/credit/config/list`

## 写接口
- `POST /arap-doc/credit/config`

## 主要参数
- 查询时 `docTypeRoot=AR`
- 账龄/预警支持 `asOfDate`
- 信用配置列表支持 `docTypeRoot`

## 前端关联
- 前端 API：`arap-doc.js`
- 前端页面：`AgingCreditView.vue`
- 路由：`/receivable/aging-credit`
