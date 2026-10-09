# 辅助核算维度余额表业务说明

## 1. 业务名称
辅助核算维度余额表

## 2. 业务定位
辅助核算维度余额表是在维度余额表基础上，再叠加科目维度，按“现金流项目 + 科目”组合展示余额。

## 3. 业务目标
- 查看辅助维度与科目的组合余额
- 支撑现金流项目与会计科目的交叉核对
- 为辅助总账、辅助明细账提供汇总入口

## 4. 数据来源
后端通过 `BizfiFiLedgerQueryServiceImpl.auxDimensionBalance` 基于已过账总账分录 `BizfiFiGlEntry` 的 `fcashflowItem + faccountCode` 组合聚合生成结果。
