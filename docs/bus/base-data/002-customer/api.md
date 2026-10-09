# 接口说明

## 客户兼容接口
- `GET /customer/list`
- `GET /customer/{fid}`
- `POST /customer`
- `PUT /customer`
- `DELETE /customer/{fid}`
- `POST /customer/{fid}/submit`
- `POST /customer/{fid}/audit`
- `POST /customer/{fid}/reject`

## BusinessPartner 身份接口
- `GET /business-partners/{fid}?tenantId=...`
- `GET /business-partners/resolve?tenantId=...&code=...`

## 前端关联
- 页面：`CustomerView.vue`
- 通用逻辑：`useSimpleData.js`
- API：`customer.js`

## 代码边界
- 旧 Customer URL 保持不变。
- 后端已切换到持久化 BusinessPartner + CUSTOMER Role。
- 无显式 tenant 的旧兼容调用 v1 使用 `default` fallback。
- CRM / Sales 新代码应使用稳定 `businessPartnerId`，不要只保存客户名称字符串。
