# P1-IMP-06 Purchase Return / Claim / Deduction 实现记录

> 状态：Implemented v1  
> 日期：2026-08-27  
> 目标分支：dev

## 1. 目标

补齐采购履约后的异常逆向业务，并把财务扣款接入现有 AP / Accounting Event / Voucher 体系：

```text
PurchaseInbound
→ PurchaseReturn
→ SupplierClaim
→ PurchaseDeduction
→ Formal AP deduction
→ Accounting Event
→ Voucher Draft
```

退货、索赔、扣款分别建模，不把库存逆向、商务索赔和财务扣款混成一个单据。

## 2. Schema

ERP 新增：

```text
matrix_erp_purchase_return
matrix_erp_purchase_return_entry
matrix_erp_supplier_claim
matrix_erp_supplier_claim_entry
matrix_erp_purchase_deduction
matrix_erp_purchase_deduction_entry
```

并在采购入库分录、采购订单分录增加累计退货数量：

```text
PurchaseInboundEntry.returnedQuantity
PurchaseOrderEntry.returnedQuantity
```

迁移：

```text
deliverables/erp/009-purchase-return-claim-deduction/schema.sql
```

FI 新增采购扣款会计消费持久化对象，迁移：

```text
deliverables/fi/009-purchase-deduction/schema.sql
```

## 3. Purchase Return

采购退货以已审核确认的 PurchaseInbound 为来源。

核心约束：

```text
returnQuantity
<= inboundEntry.quantity - inboundEntry.returnedQuantity
```

创建与确认阶段均使用来源分录行锁重新校验，避免多个并发退货单累计超退。

确认后同事务回写：

```text
PurchaseInboundEntry.returnedQuantity += returnQuantity
PurchaseOrderEntry.returnedQuantity   += returnQuantity
```

原始入库/收货事实不删除、不改写；returnedQuantity 作为独立累计逆向量保留审计轨迹。

事件：

```text
PURCHASE_RETURN_CONFIRMED
sourceDocumentType = ERP_PURCHASE_RETURN
```

已确认退货不能直接取消，后续如需逆转必须走独立冲销事实。

## 4. Supplier Claim

供应商索赔可以基于采购订单，也可以关联已确认 PurchaseReturn。

支持 v1 类型：

```text
RETURN
QUALITY
DELAY
SHORTAGE
OTHER
```

索赔分录保留：

```text
purchaseOrderEntryId
purchaseReturnEntryId(optional)
material
requestedAmount
agreedAmount
deductedAmount
```

确认时必须覆盖全部索赔分录，并校验：

```text
agreedAmount <= requestedAmount
```

确认事件：

```text
PURCHASE_CLAIM_CONFIRMED
sourceDocumentType = ERP_SUPPLIER_CLAIM
```

## 5. Purchase Deduction

采购扣款只能基于已确认、已审核的 SupplierClaim。

创建和确认阶段均按索赔分录锁定剩余可扣金额：

```text
available = agreedAmount - deductedAmount

deductionAmount <= available
```

确认后回写：

```text
SupplierClaimEntry.deductedAmount
SupplierClaim.deductedAmount
SupplierClaim.deductionStatus
```

状态：

```text
NONE
PARTIAL
COMPLETE
```

确认事件：

```text
PURCHASE_DEDUCTION_CONFIRMED
sourceDocumentType = ERP_PURCHASE_DEDUCTION
```

## 6. ERP / FI 边界

ERP 不跨库直接修改 FI 应付。

正式链路：

```text
ERP PurchaseDeduction confirmed
→ Transactional Outbox
→ PURCHASE_DEDUCTION_CONFIRMED
→ RabbitMQ
→ FI Inbox
→ PurchaseDeductionAccountingService
```

FI 消费者负责正式财务影响。

## 7. FI Formal AP Deduction

FI 按采购订单分录查找正式 AP 候选：

```text
payable.type = FORMAL
approvalStatus = AUDITED
accountingStatus in (VOUCHER_GENERATED, POSTED)
status in (OPEN, PARTIAL_SETTLED)
```

同时只允许使用未被付款申请/付款流程预留的开放余额：

```text
unreservedOpenAmount = openAmount - reservedAmount
```

并受 AP 行级剩余额度约束：

```text
lineAvailable = lineAmount - alreadyDeductedAmount
```

实际分配：

```text
allocated = min(
  deductionRemaining,
  unreservedOpenAmount,
  lineAvailable
)
```

不足时整个事务失败，不允许产生部分成功的财务扣款结果。

## 8. AP 状态更新

扣款成功后：

```text
AP.openAmount -= deduction
```

开放余额变为 0：

```text
SETTLED
```

仍有开放余额：

```text
PARTIAL_SETTLED
```

扣款不会侵占 reservedAmount，避免与 PaymentApplication / PaymentOrder 并发重复消耗同一 AP 余额。

## 9. Accounting / Voucher

FI 生成：

```text
Accounting Event:
PURCHASE_DEDUCTION_RECOGNITION
```

然后继续复用既有：

```text
AccountingRuleEngine
→ AccountingEntry / Dimension
→ Voucher Draft
→ Accounting Trace
```

规则和凭证由 FI 会计规则驱动，不在 ERP 退货/索赔服务中硬编码会计分录。

## 10. Inbox / 幂等 / Failure

FI 使用独立 consumer code：

```text
FI_PURCHASE_DEDUCTION_ACCOUNTING_V1
```

支持：

```text
PROCESSING
PROCESSED
FAILED
```

重复事件返回已处理结果；FAILED 可重置后重试。

Rabbit consumer 采用手工 ack，并通过 PurchaseDeductionInboxFailureRecorder 记录失败上下文后进入既有失败/死信机制。

## 11. API

逆向采购 API 由：

```text
PurchaseReverseController
```

统一暴露退货、索赔、扣款的创建、查询、提交、确认、驳回、取消等操作。

## 12. Tests

ERP 测试：

```text
PurchaseReverseServiceTest
PurchaseReturnServiceTest
SupplierClaimServiceTest
PurchaseDeductionServiceTest
```

覆盖：

- 超入库剩余数量退货拒绝
- 退货确认后累计退货量反写
- agreedAmount 不得超过 requestedAmount
- deduction 不得超过索赔剩余可扣金额
- claim deductionStatus 推进
- Outbox Event

FI 测试：

```text
PurchaseDeductionAccountingServiceTest
```

覆盖：

- 只扣未预留 Formal AP
- AP openAmount / status 更新
- Allocation 留痕
- Accounting Event / Voucher Draft
- Inbox processed
- 仅剩 reserved AP 余额时拒绝扣款

## 13. PR / CI

实现 PR：

```text
PR #88
feat(procurement): implement P1 purchase return claim deduction
```

合并提交：

```text
d11e6571957432a0aeab4cf515c54b16f699278f
```

合并前最新 head：

```text
34fcaf11f96287ad082ac18b3a363be0b590b82c
```

最新 head 已通过：

```text
Repository Hygiene CI          success
Biz Finance P0 CI              success
Finance Workflow Integration CI success
OpenAPI CI                     success
BOTP CI                        success
```

Scheduler Reliability CI 在合并时仍独立执行中；本次变更没有修改 scheduler 模块，且仓库允许在该非阻塞检查运行时合并。

## 14. 当前边界

P1-IMP-06 v1 已完成：

```text
Inbound
→ Return
→ Claim
→ Deduction
→ Formal AP deduction
→ Accounting Event
→ Voucher Draft
```

暂不包含：

- 已确认退货冲销
- 已确认扣款冲销
- Supplier Portal
- 自动索赔规则/罚则计算引擎
- 贷项通知单/税务红字发票完整链
- Procurement BOTP Relation 全量补齐

## 15. 下一阶段

```text
P1-IMP-07 Procurement BOTP Completion
```

目标：把 PurchaseRequest、RFQ、Award、Contract、PurchaseOrder、DeliveryPlan、Receipt、Acceptance、Inbound、Return、Claim、Deduction 的正式上下游关系统一落入 BOTP Relation，并补齐转换幂等和反写关系。
