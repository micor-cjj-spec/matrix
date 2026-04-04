# 应收管理业务文档索引（ar）

本目录已按当前前后端代码中的真实菜单与路由整理。

## 当前模块
- `001-receivable`：应收
- `002-estimated-receivable`：暂估应收
- `003-settlement-processing`：结算处理
- `004-aging-credit-warning`：账龄与信用预警

## 代码对齐说明
- 单据页统一复用：`ArapDocView.vue`
- 预警页统一复用：`AgingCreditView.vue`
- 后端统一接口：`/arap-doc/*`

## 主要路由
- `/receivable/manage`
- `/receivable/estimate`
- `/receivable/settlement`
- `/receivable/aging-credit`
