# 接口说明

## 行名行号接口
- `GET /bank-info/list`
- `GET /bank-info/{fid}`
- `POST /bank-info`
- `PUT /bank-info`
- `DELETE /bank-info/{fid}`
- `POST /bank-info/{fid}/submit`
- `POST /bank-info/{fid}/audit`
- `POST /bank-info/{fid}/reject`

## 前端关联
- 前端页面：`BankInfoView.vue`
- 通用逻辑：`useSimpleData.js`
- 前端 API：`bankInfo.js`

## 代码边界
- 前端接口调用已接入。
- 本轮未检到对应后端实现文件。
