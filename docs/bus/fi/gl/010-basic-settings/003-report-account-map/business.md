# 报表科目映射业务说明

## 1. 业务名称
报表科目映射

## 2. 业务定位
报表科目映射用于维护报表模板、报表项目与会计科目的显式映射关系，优先级高于系统兜底规则。

## 3. 业务目标
- 维护报表项目与会计科目的明确归属
- 支撑报表生成时优先命中人工映射
- 通过期间控制映射生效范围

## 4. 数据来源
前端 `ReportAccountMapView.vue` 调用 `/report-account-map/*` 接口；后端由 `BizfiFiReportAccountMapController / ServiceImpl` 基于 `bizfi_fi_report_account_map` 表完成 CRUD。
