# 往来通知单业务说明

## 1. 业务名称
往来通知单

## 2. 业务定位
往来通知单用于把应收/应付账龄与风险扫描结果转化为内部处理单，推动团队跟进超额度、超期和长期未清问题。

## 3. 业务目标
- 从最新往来风险扫描结果生成内部通知单
- 统一跟踪通知状态、紧急程度、处理建议和快照金额
- 为对账单和通知单勾稽提供处理入口

## 4. 数据来源
后端通过 `BizfiFiInternalNoticeServiceImpl.generateCounterpartyNotices/queryCounterpartyNotices`，基于往来账龄分析与内部通知单表生成结果。
