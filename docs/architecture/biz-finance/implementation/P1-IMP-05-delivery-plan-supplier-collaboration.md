# P1-IMP-05 DeliveryPlan / Supplier Collaboration 实现记录

> 状态：Implemented v1  
> 日期：2026-08-27  
> 目标分支：dev

## 1. 目标

补齐 PurchaseOrder 与 PurchaseReceipt 之间缺失的交付计划和供应商协同事实：

```text
PurchaseOrder
→ PurchaseDeliveryPlan
→ SupplierDeliveryResponse
→ PurchaseReceipt
```

不改写现有 P0 收货逻辑，而是在收货前增加受控计划层。

## 2. Schema

新增：

```text
matrix_erp_purchase_delivery_plan
matrix_erp_purchase_delivery_plan_entry
matrix_erp_supplier_delivery_response
matrix_erp_supplier_delivery_response_entry
```

迁移：

```text
deliverables/erp/008-delivery-plan-collaboration/schema.sql
```

取消的计划保留历史；同一 PO 同时只允许一个未取消计划。

## 3. Delivery Plan

计划必须基于：

```text
PurchaseOrder.status = EFFECTIVE
PurchaseOrder.approvalStatus = AUDITED
PurchaseOrder.closeStatus = OPEN
```

计划必须完整覆盖采购订单全部分录数量。

同一个 PO Entry 可以拆成多个计划行：

```text
PO Entry 10
→ 4 @ 2026-09-10
→ 6 @ 2026-09-20
```

状态：

```text
DRAFT
PUBLISHED
CHANGE_PROPOSED
CONFIRMED
PARTIAL
COMPLETE
REJECTED
CANCELLED
```

## 4. Supplier Collaboration

供应商响应独立留历史，不直接覆盖计划事实。

支持：

```text
CONFIRM
CHANGE
REJECT
```

CONFIRM：

- 原样确认采购方数量和日期。
- 立即形成最新供应商承诺。

CHANGE：

- 可以调整分批数量和日期。
- 同一 PO Entry 的承诺总量必须保持等于订单总量。
- 已收货数量不能被新承诺量反向覆盖。
- 采购方必须显式 accept 后才成为最新承诺。

REJECT：

- 计划进入 REJECTED。
- 可重新编辑后再次发布。

## 5. 收货约束

有未取消 DeliveryPlan 的 PO：

- 供应商承诺未确认时禁止创建收货预占。
- 收货预占不能超过：
  `committedQuantity - receivedQuantity - existingReceiptReservedQuantity`
- 收货确认按 committedDeliveryDate FIFO 分配到计划行。
- DeliveryPlanEntry.receivedQuantity 同事务反写。

没有 DeliveryPlan 的历史/手工 PO 保持原有收货兼容。

## 6. 并发规则

关键路径使用行锁：

```text
PurchaseOrder / PurchaseOrderEntry
PurchaseDeliveryPlan
PurchaseDeliveryPlanEntry
```

已有收货预占时禁止 Supplier CHANGE 和计划取消，避免“收货在途”和供应商改承诺并发冲突。

## 7. 执行状态

收货反写后：

```text
CONFIRMED
→ PARTIAL
→ COMPLETE
```

COMPLETE 表示全部供应商承诺数量已实际收货。

## 8. Events

Transactional Outbox 新增：

```text
PURCHASE_DELIVERY_PLAN_PUBLISHED
→ biz.procurement.delivery_plan.published

SUPPLIER_DELIVERY_RESPONSE_RECORDED
→ biz.procurement.delivery_response.recorded

PURCHASE_DELIVERY_PLAN_CONFIRMED
→ biz.procurement.delivery_plan.confirmed
```

## 9. API

```text
POST /procurement/delivery-plans
PUT  /procurement/delivery-plans/{fid}
GET  /procurement/delivery-plans/{fid}
GET  /procurement/delivery-plans

POST /procurement/delivery-plans/{fid}/publish
POST /procurement/delivery-plans/{fid}/cancel

POST /procurement/delivery-plans/{fid}/supplier-responses
GET  /procurement/delivery-plans/{fid}/supplier-responses

POST /procurement/delivery-plans/{fid}/supplier-responses/{responseId}/accept
POST /procurement/delivery-plans/{fid}/supplier-responses/{responseId}/reject
```

## 10. Tests

新增：

```text
PurchaseDeliveryPlanServiceTest
DeliveryPlanFulfillmentServiceTest
```

并调整：

```text
PurchaseOrderFulfillmentServiceTest
```

覆盖：

- PO 完整计划数量校验
- 发布事件
- Supplier CONFIRM
- Supplier CHANGE 不允许减少总承诺量
- Supplier REJECT
- Delivery Commitment 收货预占校验
- FIFO 收货分配
- DeliveryPlan COMPLETE
- 无计划旧 PO 的兼容性

## 11. PR / CI

实现 PR：

```text
PR #87
feat(procurement): implement P1 delivery plan supplier collaboration
```

合并提交：

```text
087e615de9c120144e35e36492f088ddd20ce674
```

PR 门禁：

```text
Repository Hygiene CI  33055019332 success
Biz Finance P0 CI      33055019239 success
```

## 12. 当前边界

P1-IMP-05 已完成后端事实闭环。

暂不包含：

- Supplier Portal UI
- 邮件/IM 供应商通知
- ASN / Advanced Shipping Notice
- 运输轨迹
- 完整 Procurement BOTP Relation
- 供应商绩效评分

## 13. 下一阶段

```text
P1-IMP-06 Purchase Return / Claim / Deduction
```

下一阶段补齐收货/验收后的异常逆向业务：
采购退货、供应商索赔、扣款与后续财务影响。
