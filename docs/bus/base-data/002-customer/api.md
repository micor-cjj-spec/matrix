# 接口说明

## 客户接口
- `GET /customer/list`
- `GET /customer/{fid}`
- `POST /customer`
- `PUT /customer`
- `DELETE /customer/{fid}`
- `POST /customer/{fid}/submit`
- `POST /customer/{fid}/audit`
- `POST /customer/{fid}/reject`

## 前端关联
- 前端页面：`CustomerView.vue`
- 通用逻辑：`useSimpleData.js`
- 前端 API：`customer.js`

## 代码边界
- 前端接口调用已接入。
- 本轮未检到对应后端实现文件。
