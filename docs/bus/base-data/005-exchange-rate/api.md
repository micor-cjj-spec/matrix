# 接口说明

## 汇率接口
- `GET /exchange-rate/list`
- `GET /exchange-rate/{fid}`
- `POST /exchange-rate`
- `PUT /exchange-rate`
- `DELETE /exchange-rate/{fid}`
- `POST /exchange-rate/{fid}/submit`
- `POST /exchange-rate/{fid}/audit`
- `POST /exchange-rate/{fid}/reject`

## 前端关联
- 前端页面：`ExchangeRateView.vue`
- 通用逻辑：`useSimpleData.js`
- 前端 API：`exchangeRate.js`

## 代码边界
- 前端接口调用已接入。
- 本轮未检到对应后端实现文件。
