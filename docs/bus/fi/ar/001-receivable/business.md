# 应收业务说明

## 1. 业务名称
应收

## 2. 业务定位
应收单用于记录客户应收确认，是 AR/AP 通用单据页在 `docType=AR` 场景下的主营业务单据。

## 3. 业务目标
- 维护应收单据基础信息
- 支持草稿、提交、审核、驳回流转
- 审核通过后自动生成并关联总账凭证

## 4. 数据来源
前端 `ArapDocView.vue` 以 `/receivable/manage` 路由承载，应收场景通过 `/arap-doc/*` 接口完成；后端由 `BizfiFiArapDocController / ServiceImpl` 基于 `bizfi_fi_arap_doc` 表实现。
