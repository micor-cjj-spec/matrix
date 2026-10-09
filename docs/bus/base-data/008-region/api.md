# 接口说明

## 地区接口
- `GET /region/list`
- `GET /region/{fid}`
- `POST /region`
- `PUT /region`
- `DELETE /region/{fid}`
- `POST /region/{fid}/submit`
- `POST /region/{fid}/audit`
- `POST /region/{fid}/reject`

## 前端关联
- 前端页面：`RegionView.vue`
- 通用逻辑：`useSimpleData.js`
- 前端 API：`region.js`

## 代码边界
- 前端接口调用已接入。
- 本轮未检到对应后端实现文件。
