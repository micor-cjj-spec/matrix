# 接口说明

## 币种接口
- `GET /currency/list`
- `GET /currency/{fid}`
- `POST /currency`
- `PUT /currency`
- `DELETE /currency/{fid}`
- `POST /currency/{fid}/submit`
- `POST /currency/{fid}/audit`
- `POST /currency/{fid}/reject`

## 前端关联
- 前端页面：`CurrencyView.vue`
- 通用逻辑：`useSimpleData.js`
- 前端 API：`currency.js`

## 代码边界
- 前端接口调用已接入。
- 本轮未检到对应后端实现文件。
