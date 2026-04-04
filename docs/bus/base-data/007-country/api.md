# 接口说明

## 国家接口
- `GET /country/list`
- `GET /country/{fid}`
- `POST /country`
- `PUT /country`
- `DELETE /country/{fid}`
- `POST /country/{fid}/submit`
- `POST /country/{fid}/audit`
- `POST /country/{fid}/reject`

## 前端关联
- 前端页面：`CountryView.vue`
- 通用逻辑：`useSimpleData.js`
- 前端 API：`country.js`

## 代码边界
- 前端接口调用已接入。
- 本轮未检到对应后端实现文件。
