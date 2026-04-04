# 接口说明

## 计量单位接口
- `GET /unit/list`
- `GET /unit/{fid}`
- `POST /unit`
- `PUT /unit`
- `DELETE /unit/{fid}`
- `POST /unit/{fid}/submit`
- `POST /unit/{fid}/audit`
- `POST /unit/{fid}/reject`

## 前端关联
- 前端页面：`UnitView.vue`
- 通用逻辑：`useSimpleData.js`
- 前端 API：`unit.js`

## 代码边界
- 前端接口调用已接入。
- 本轮未检到对应后端实现文件。
