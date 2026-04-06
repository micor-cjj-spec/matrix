# 现金流量表口径说明

## 1. 查询条件口径
- `orgId`：业务单元
- `period`：期间
- `currency`：币种
- `templateId`：模板ID
- `showZero`：是否显示零值行

## 2. 报表结果口径
- 报表按经营、投资、筹资三大活动分类展示
- 计算本期现金净流量
- 计算现金及现金等价物净增加额
- `templateId` / `templateName` 用于标识当前报表模板

## 3. checks 与 warnings 口径
- `checks`：用于展示报表质量校验结果
- `warnings`：用于提示现金划转、未知编码、启发式分类等影响报表质量的问题

## 4. 跳转口径
- “现金流量查询”跳转到 `/ledger/cash-flow-query`
- “补充资料”跳转到 `/ledger/cash-flow-supplement`

## 5. 待确认项
- checks 的正式分级规则是否需要单独固化
- 启发式分类的最终判定口径是否需要更细说明
