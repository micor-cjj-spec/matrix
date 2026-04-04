# 应付管理业务文档索引（ap）

本目录已按当前前后端代码中的真实菜单与路由整理。

## 当前模块
- `001-payable`：应付
- `002-estimated-payable`：暂估应付
- `003-payment-application`：付款申请
- `004-payment-processing`：付款处理
- `005-aging-credit-warning`：账龄与信用预警

## 代码对齐说明
- 单据页统一复用：`ArapDocView.vue`
- 预警页统一复用：`AgingCreditView.vue`
- 后端统一接口：`/arap-doc/*`

## 主要路由
- `/payable/manage`
- `/payable/estimate`
- `/payable/application`
- `/payable/processing`
- `/payable/aging-credit`
