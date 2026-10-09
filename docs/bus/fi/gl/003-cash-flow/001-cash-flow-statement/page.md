# 页面说明

前端页面：`CashFlowView.vue`

## 页面组成
- 查询区：业务单元、期间、币种、模板ID、显示零值行
- 告警区：展示 `warnings`
- 校验区：展示 `checks`
- 表格区：展示现金流量表行项目
- 跳转按钮：现金流量查询、补充资料

## 页面行为
- 初始化时加载业务单元并自动查询
- 点击“查询”调用现金流量表接口
- 点击“现金流量查询”跳转 `/ledger/cash-flow-query`
- 点击“补充资料”跳转 `/ledger/cash-flow-supplement`
