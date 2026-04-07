# 资产负债表评审清单

## 1. 一致性检查
- BUS 与 prompt 是否一致
- 资产总计、负债和所有者权益、差额、平衡状态、warnings、checks 口径是否一致
- 初始化自动查询、查询、重置、下钻展示是否一致
- showZero 的展示边界是否一致

## 2. 已对齐项
- 前端页面为 `BalanceSheetView.vue`
- 前端已接入 `GET /balance-sheet`、`GET /balance-sheet/drill`
- 前端已接入元信息条、汇总卡片、告警区、校验区、主表区、下钻弹窗

## 3. 当前边界
- 本轮未在仓库中检出对应后端实现文件
- 当前文档不展开后端内部 controller / service / mapper 细节

## 4. 待确认项
- `checks` 的具体结构与分级是否需要单独文档化
- 下钻明细是否包含更多辅助分析维度
