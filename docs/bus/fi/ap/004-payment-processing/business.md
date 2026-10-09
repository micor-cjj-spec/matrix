# 付款处理业务说明

## 1. 业务名称
付款处理

## 2. 业务定位
付款处理用于记录付款执行结果，是 AR/AP 通用单据页在 `docType=AP_PAYMENT_PROCESS` 场景下的差异化业务。

## 3. 业务目标
- 维护付款处理单据
- 承接付款申请后的执行结果
- 审核通过后自动生成并关联总账凭证

## 4. 数据来源
前端 `ArapDocView.vue` 以 `/payable/processing` 路由承载；后端仍通过 `/arap-doc/*` 接口和 `bizfi_fi_arap_doc` 表实现。
