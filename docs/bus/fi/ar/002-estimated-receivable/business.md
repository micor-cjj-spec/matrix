# 暂估应收业务说明

## 1. 业务名称
暂估应收

## 2. 业务定位
暂估应收用于记录尚未形成正式应收但需要先行确认的应收预估，是 AR/AP 通用单据页在 `docType=AR_ESTIMATE` 场景下的差异化业务。

## 3. 业务目标
- 维护暂估应收单据
- 记录暂估来源单号
- 审核通过后自动生成并关联总账凭证

## 4. 数据来源
前端 `ArapDocView.vue` 以 `/receivable/estimate` 路由承载；后端仍通过 `/arap-doc/*` 接口和 `bizfi_fi_arap_doc` 表实现。
