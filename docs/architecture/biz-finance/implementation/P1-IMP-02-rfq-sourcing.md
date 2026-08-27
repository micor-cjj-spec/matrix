# P1-IMP-02 RFQ / 采购询价与比价实现记录

> 状态：Implemented v1  
> 日期：2026-08-27  
> 目标分支：dev

## 1. 目标

承接已审批生效的 PurchaseRequest，将采购需求转为正式寻源事实，形成：

```text
PurchaseRequest APPROVED
→ RFQ
→ Supplier Invitation
→ Supplier Quote
→ Comparison
→ Sourcing Award
→ PurchaseRequest sourcedQuantity writeback
```

本阶段不把供应商报价字段塞回 PurchaseRequest，也不实现复杂评分策略或审批路由。

## 2. Schema

新增交付：

```text
deliverables/erp/005-procurement-sourcing/schema.sql
```

新增表：

```text
matrix_erp_procurement_rfq
matrix_erp_procurement_rfq_entry
matrix_erp_procurement_rfq_supplier
matrix_erp_supplier_quote
matrix_erp_supplier_quote_entry
matrix_erp_sourcing_award
matrix_erp_sourcing_award_entry
```

全部遵守 Matrix 数据库命名规范。

## 3. RFQ

RFQ v1 支持：

- 从 APPROVED + EFFECTIVE 的采购申请行创建询价需求。
- 同一 RFQ 可包含多个采购申请行。
- 邀请多个 Business Partner 供应商。
- 发布后 PurchaseRequest.executionStatus 从 NONE 推进到 SOURCING。
- 报价截止时间校验。
- DRAFT / PUBLISHED / CLOSED / CANCELLED 状态。
- v1 仅允许取消未发布草稿。

RFQ 数量不得超过采购申请行当前未寻源数量。

## 4. Supplier Quote

供应商报价支持：

- 仅邀请供应商可报价。
- 同一 RFQ / Supplier v1 保留一份有效报价。
- 按 RFQ 行报价。
- 数量、未税单价、税率、交期。
- 后端统一计算 net / tax / gross。
- DRAFT → SUBMITTED。
- 超过 RFQ 报价截止时间不允许提交。

税率统一按 0-1 小数语义，例如 13% = 0.13。

## 5. Comparison

接口按已提交报价形成逐行比较结果。

当前 v1 输出：

```text
supplier
quantity
unitPrice
taxRate
grossUnitPrice
grossAmount
deliveryDate
lowestGrossUnitPrice
```

`lowestGrossUnitPrice` 仅作为比较事实，不自动决定中标供应商。

复杂权重评分、技术评分、商务评分不在 v1 硬编码。

## 6. Award

定标支持按询价行引用已提交的 SupplierQuoteEntry。

定标时事务内同时校验三层剩余量：

```text
RFQ Entry remaining
Quote Entry remaining
PurchaseRequest Entry remaining unsourced
```

任何一层不足均拒绝定标，避免并发超采/超定标。

定标成功后：

```text
RFQEntry.awardedQuantity += awarded
QuoteEntry.awardedQuantity += awarded
PurchaseRequestEntry.sourcedQuantity += awarded
```

采购申请全部行完成寻源时：

```text
PurchaseRequest.executionStatus = CONTRACTING
```

否则保持：

```text
SOURCING
```

RFQ 全部分录完成定标后：

```text
RFQ.status = CLOSED
```

## 7. Business Event

定标事实与业务数据处于同一 ERP 本地事务，通过 Transactional Outbox 发送：

```text
PURCHASE_SOURCING_AWARDED
routingKey:
biz.procurement.sourcing.awarded
```

source document：

```text
ERP_SOURCING_AWARD
```

payload 包含 RFQ、Award、采购申请来源、供应商、数量、价格、税率和含税金额快照。

## 8. API

```text
POST /procurement/rfqs
GET  /procurement/rfqs
GET  /procurement/rfqs/{fid}

POST /procurement/rfqs/{fid}/publish
POST /procurement/rfqs/{fid}/cancel

POST /procurement/rfqs/{fid}/quotes
POST /procurement/rfqs/{fid}/quotes/{quoteId}/submit
GET  /procurement/rfqs/{fid}/quotes
GET  /procurement/rfqs/{fid}/comparison

POST /procurement/rfqs/{fid}/awards
```

## 9. 并发与一致性

为采购申请行增加 `SELECT ... FOR UPDATE`。

关键写路径锁定：

- RFQ Entry
- Supplier Quote / Quote Entry
- PurchaseRequest / PurchaseRequestEntry

SourcingAward、寻源量反写和 Outbox Event 同事务提交。

## 10. Tests

新增：

```text
ProcurementSourcingServiceTest
```

覆盖：

1. 未审批采购申请禁止创建有效寻源。
2. RFQ 发布后采购申请进入 SOURCING。
3. 报价税额与含税金额计算。
4. 比价标记最低含税单价但不强制定标。
5. 定标后 sourcedQuantity 回写。
6. 全部寻源完成后推进 CONTRACTING。
7. RFQ 全部定标后 CLOSED。
8. 定标事件写入 Outbox。
9. 超过采购申请剩余量时拒绝定标。

## 11. Commit / CI

实现 PR：

```text
PR #84
feat(procurement): implement P1 RFQ sourcing and award
```

合并提交：

```text
b2a12dbc72eaf14ac2ffae4f03ded0a1030f1abb
```

CI：

```text
Biz Finance P0 CI      33052468541 success
Repository Hygiene CI 33052468545 success
```

## 12. 当前边界

P1-IMP-02 v1 已完成后端领域闭环。

未在本阶段实现：

- 复杂供应商评分模型
- 审批路由
- 询价策略引擎
- 合同条款
- 自动生成采购订单
- Supplier Portal
- BOTP 全量关系补齐

这些继续按照 P1 路线拆分。

## 13. 下一阶段

```text
P1-IMP-03 PurchaseContract
```

目标：基于已确认的 SourcingAward 建立采购合同业务对象，为后续 Contract → PurchaseOrder 和合同执行控制提供正式业务事实。
