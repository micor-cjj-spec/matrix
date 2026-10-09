# 现金流量项目业务说明

## 1. 业务名称
现金流量项目

## 2. 业务定位
现金流量项目是现金流主数据维护页，用于维护现金流项目编码、名称、分类、方向、层级和状态，供凭证分录标记、现金流查询和现金流量表归集使用。

## 3. 业务目标
- 维护现金流项目主数据
- 为凭证分录提供显式现金流项目编码
- 为现金流量表和现金流量查询提供分类基础

## 4. 数据来源
前端 `CashflowItemView.vue` 调用 `/cashflow-item/*` 接口；后端由 `BizfiFiCashflowItemController / ServiceImpl` 基于 `bizfi_fi_cashflow_item` 表完成 CRUD。
