# 付款申请业务说明

## 1. 业务名称
付款申请

## 2. 业务定位
付款申请用于记录对供应商的付款申请，是 AR/AP 通用单据页在 `docType=AP_PAYMENT_APPLY` 场景下的差异化业务。

## 3. 业务目标
- 维护付款申请单据
- 记录付款方式和预计付款日
- 审核通过后自动生成并关联总账凭证

## 4. 数据来源
前端 `ArapDocView.vue` 以 `/payable/application` 路由承载；后端仍通过 `/arap-doc/*` 接口和 `bizfi_fi_arap_doc` 表实现。
