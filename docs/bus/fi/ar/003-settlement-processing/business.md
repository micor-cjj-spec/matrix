# 结算处理业务说明

## 1. 业务名称
结算处理

## 2. 业务定位
结算处理用于记录客户回款或结算确认，是 AR/AP 通用单据页在 `docType=AR_SETTLEMENT` 场景下的差异化业务。

## 3. 业务目标
- 维护结算处理单据
- 记录结算方式和核销明细
- 审核通过后自动生成并关联总账凭证

## 4. 数据来源
前端 `ArapDocView.vue` 以 `/receivable/settlement` 路由承载；后端仍通过 `/arap-doc/*` 接口和 `bizfi_fi_arap_doc` 表实现。
