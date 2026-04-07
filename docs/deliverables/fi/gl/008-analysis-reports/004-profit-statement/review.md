# 利润表评审清单

## 1. 一致性检查
- BUS 与 prompt 是否一致
- 本期金额、本年累计金额、warnings、checks 口径是否一致
- 初始化自动查询、查询、showZero 展示是否一致
- 当前未接入项目下钻弹窗的边界是否一致

## 2. 已对齐项
- 前端页面为 `ProfitStatementView.vue`
- 前端已接入 `GET /profit-statement`
- 前端已接入查询区、告警区、校验区、主表区
- 当前页面未接入项目下钻弹窗

## 3. 当前边界
- 本轮未在仓库中检出对应后端实现文件
- 当前文档不展开后端内部 controller / service / mapper 细节

## 4. 待确认项
- `checks` 的具体结构与分级是否需要单独文档化
- 利润表是否后续会扩展为支持项目下钻
