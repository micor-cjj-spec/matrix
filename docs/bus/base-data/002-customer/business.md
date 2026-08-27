# 客户业务说明

## 1. 业务名称
客户

## 2. 业务定位
客户模块用于维护统一 BusinessPartner 主体的 CUSTOMER 角色，支持创建、编辑、提交审核、审核通过、驳回和删除。

## 3. 当前实现
- 前端页面：`CustomerView.vue`
- 前端通用逻辑：`useSimpleData.js`
- 前端 API：`src/api/customer.js`
- 后端兼容入口：`PartnerCompatibilityController`
- 后端领域服务：`BusinessPartnerService`
- 权威主体：`matrix_base_business_partner`
- 客户角色：`matrix_base_business_partner_role(role=CUSTOMER)`

Customer 不再由 `BaseDataWorkflowController` 的内存 Map 保存。

同一法人同时是客户和供应商时，共用一个 BusinessPartner，只增加不同 Role。

内部生命周期与审批状态分离；为兼容现有页面，兼容接口的 `fstatus` 继续返回审批状态。
