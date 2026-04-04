# 接口说明

## 物料接口
- `GET /material/list`
- `GET /material/{fid}`
- `POST /material`
- `PUT /material`
- `DELETE /material/{fid}`
- `POST /material/{fid}/submit`
- `POST /material/{fid}/audit`
- `POST /material/{fid}/reject`

## 前端关联
- 前端页面：`MaterialView.vue`
- 通用逻辑：`useSimpleData.js`
- 前端 API：`material.js`

## 代码边界
- 前端接口调用已接入。
- 本轮未检到对应后端实现文件。
