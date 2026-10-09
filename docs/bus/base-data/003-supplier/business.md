# 供应商业务说明

## 1. 业务名称
供应商

## 2. 业务定位
供应商模块用于维护统一 BusinessPartner 主体的 SUPPLIER 角色，支持创建、编辑、提交审核、审核通过、驳回和删除。

## 3. 当前实现
- 前端页面：`SupplierView.vue`
- 前端通用逻辑：`useSimpleData.js`
- 前端 API：`src/api/supplier.js`
- 后端兼容入口：`PartnerCompatibilityController`
- 后端领域服务：`BusinessPartnerService`
- 权威主体：`matrix_base_business_partner`
- 供应商角色：`matrix_base_business_partner_role(role=SUPPLIER)`

Supplier 不再由 `BaseDataWorkflowController` 的内存 Map 保存。

同一法人同时是客户和供应商时，共用一个 BusinessPartner，只增加不同 Role。

采购交易继续保存 businessPartnerId + code/name 交易快照。
