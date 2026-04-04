# 接口说明

## 供应商接口
- `GET /supplier/list`
- `GET /supplier/{fid}`
- `POST /supplier`
- `PUT /supplier`
- `DELETE /supplier/{fid}`
- `POST /supplier/{fid}/submit`
- `POST /supplier/{fid}/audit`
- `POST /supplier/{fid}/reject`

## 前端关联
- 前端页面：`SupplierView.vue`
- 通用逻辑：`useSimpleData.js`
- 前端 API：`supplier.js`

## 代码边界
- 前端接口调用已接入。
- 本轮未检到对应后端实现文件。
