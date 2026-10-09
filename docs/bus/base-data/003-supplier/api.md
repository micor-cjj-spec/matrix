# 接口说明

## 供应商兼容接口
- `GET /supplier/list`
- `GET /supplier/{fid}`
- `POST /supplier`
- `PUT /supplier`
- `DELETE /supplier/{fid}`
- `POST /supplier/{fid}/submit`
- `POST /supplier/{fid}/audit`
- `POST /supplier/{fid}/reject`

## BusinessPartner 身份接口
- `GET /business-partners/{fid}?tenantId=...`
- `GET /business-partners/resolve?tenantId=...&code=...`

## 前端关联
- 页面：`SupplierView.vue`
- 通用逻辑：`useSimpleData.js`
- API：`supplier.js`

## 代码边界
- 旧 Supplier URL 保持不变。
- 后端已切换到持久化 BusinessPartner + SUPPLIER Role。
- 无显式 tenant 的旧兼容调用 v1 使用 `default` fallback。
- 新采购/供应商协同代码应引用稳定 `businessPartnerId`。
