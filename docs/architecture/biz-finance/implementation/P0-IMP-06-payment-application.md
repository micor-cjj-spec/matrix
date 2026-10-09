# P0-IMP-06 PaymentApplication 实现记录

> 状态：Implemented v1  
> 日期：2026-08-27  
> 目标分支：`dev`

## 1. 目标

本阶段实现正式应付到付款申请的第一段资金链：

~~~text
Formal AP
→ PaymentApplication
→ AP Reservation
→ Evidence Check
→ Budget Check Result
→ Submit / Approve
→ PAYMENT_APPLICATION_APPROVED
~~~

本阶段不执行银行支付、不减少 AP open amount、不生成付款凭证。

后续阶段：

~~~text
P0-IMP-07 PaymentOrder + BankTransaction
P0-IMP-08 AP Settlement + Payment Voucher
~~~

## 2. 核心边界

PaymentApplication 表示“请求支付，并占用可支付应付余额”；PaymentOrder 表示“财务/出纳真正执行的支付指令”。

~~~text
PaymentApplication APPROVED
≠ PaymentOrder PAID
≠ AP Settled
~~~

## 3. AP 余额模型

`matrix_fi_ap_payable` 新增：

~~~text
freserved_amount
fdue_date
fpayment_term_code
~~~

金额语义：

~~~text
openAmount     = 尚未完成 Settlement 的应付余额
reservedAmount = 已被有效付款申请占用、但尚未支付/核销的金额
availableAmount = openAmount - reservedAmount
~~~

PaymentApplication 生命周期只改变 reservation；后续 Settlement 才改变 `fsettled_amount / fopen_amount`。

## 4. 新增数据对象

DDL：

~~~text
deliverables/fi/006-payment-application/schema.sql
~~~

新增：

~~~text
matrix_fi_payment_application
matrix_fi_payment_application_allocation
matrix_fi_payment_application_evidence
matrix_fi_business_event_outbox
~~~

PaymentApplication 状态：

~~~text
DRAFT → SUBMITTED → APPROVED
SUBMITTED → REJECTED
DRAFT / REJECTED → CANCELLED
REJECTED → SUBMITTED
~~~

Allocation 状态：

~~~text
RESERVED / RELEASED / CONSUMED
~~~

一张付款申请可关联多个 Formal AP，一张 AP 也可多次申请付款。

## 5. Reservation

创建付款申请即预占余额：

~~~text
Formal AP:
open = 10000
reserved = 0

PaymentApplication = 6000

→ open = 10000
→ reserved = 6000
→ available = 4000
~~~

并发控制：

~~~text
按 payableId 排序
→ SELECT ... FOR UPDATE
→ 校验最新 open / reserved
→ 更新 reserved
→ 创建 Application / Allocation
→ 同事务提交
~~~

源 AP 必须：

~~~text
type = FORMAL
status = OPEN / PARTIAL_SETTLED
approvalStatus = AUDITED
accountingStatus = VOUCHER_GENERATED / POSTED
requestedAmount <= openAmount - reservedAmount
~~~

同一申请的 AP 必须保持相同 org / BusinessPartner / currency。

## 6. 驳回、取消与重新提交

Reject：

~~~text
SUBMITTED → REJECTED
→ release Allocation reservation
→ AP.reservedAmount 回退
~~~

Cancel：

~~~text
DRAFT / REJECTED → CANCELLED
→ release reservation
~~~

BOTP 来源的付款申请取消后：

~~~text
FI_PAYMENT_APPLICATION / PA:{fid}
→ target-status CANCELLED
→ BOTP Relation lifecycle
~~~

业务取消已经提交后，BOTP 回调失败不回滚业务取消，由 BOTP reconciliation 补偿。

Resubmit：

~~~text
REJECTED
→ lock AP
→ 按当前 availableAmount 重新 reserve
→ controls 再校验
→ SUBMITTED
~~~

## 7. Evidence

表：

~~~text
matrix_fi_payment_application_evidence
~~~

第一批类型：

~~~text
CONTRACT
INVOICE
ACCEPTANCE
INBOUND
RECONCILIATION
OTHER
~~~

每条 Evidence 有 `required` 与：

~~~text
PENDING / VERIFIED / REJECTED
~~~

聚合：

~~~text
required 任一 REJECTED → FAILED
required 全部 VERIFIED → PASSED
否则 → PENDING
~~~

没有 required evidence 时保持 PENDING，不能提交。

## 8. Budget / Fund Plan

PaymentApplication 不实现 Budget Domain 算法，只消费：

~~~text
PENDING / PASSED / FAILED / NOT_REQUIRED
~~~

PASSED 时要求：

~~~text
availableAmount >= PaymentApplication.amount
~~~

并保存 checkId / fundPlanId / availableAmount / message / snapshot。

## 9. Submit Gate

提交必须满足：

~~~text
evidenceCheckStatus = PASSED
budgetCheckStatus = PASSED / NOT_REQUIRED
所有 Allocation.status = RESERVED
reservedAmount = appliedAmount
~~~

否则阻断。

## 10. Approval

审批通过：

~~~text
SUBMITTED → APPROVED
approvalStatus = AUDITED
~~~

但：

~~~text
AP.openAmount 不变
AP.reservedAmount 保持
不生成付款凭证
不表示银行已支付
~~~

P0 v1 提供领域审批动作；后续 workflow-service 可调用/回调该领域动作，不把工作流状态作为 AP/资金余额权威状态。

## 11. FI Transactional Outbox

新增：

~~~text
matrix_fi_business_event_outbox
FiBusinessEventOutboxService
FiBusinessEventOutboxDispatcher
~~~

模式：

~~~text
business state update + outbox append
same local transaction
→ claim
→ RabbitMQ publisher confirm
→ PUBLISHED / FAILED / DEAD
~~~

支持 claim token、stale claim recovery、retry、required routing 和 publisher confirm。

## 12. PAYMENT_APPLICATION_APPROVED

审批与 Outbox 同事务：

~~~text
PAYMENT_APPLICATION_APPROVED
→ biz.finance.payment_application.approved
~~~

Payload 包含申请、客商、币种、金额、计划付款日、付款方式、资金计划、预算检查和 AP allocations；不把收款银行账号明文放入 Business Event Payload。

该事件不触发财务核算。

## 13. BOTP 新旧兼容

历史：

~~~text
FI_AP_DOC
→ AP_TO_PAYMENT_APPLICATION
→ legacy bizfi_fi_arap_doc
~~~

新主链：

~~~text
FI_AP_PAYABLE
→ FORMAL_AP_TO_PAYMENT_APPLICATION
→ canonical matrix_fi_payment_application
~~~

新增：

~~~text
FiFormalPayableDocumentAdapter
FiPaymentApplicationClient
FORMAL_AP_TO_PAYMENT_APPLICATION
~~~

规范付款申请 Target ID：

~~~text
PA:{fid}
~~~

避免与历史 `FI_PAYMENT_APPLICATION` 数字 ID 混淆。

## 14. BOTP Writeback

新链不把 BOTP activeAllocatedAmount 当作已付款/已核销。

目标创建时 PaymentApplicationService 已完成 reservation；Relation 保存后：

~~~text
FiFormalPayableDocumentAdapter.applyWriteback
→ recomputePayableReservation
→ SUM canonical allocations WHERE status=RESERVED
→ AP.freserved_amount
~~~

边界：

~~~text
BOTP Relation = 文档关系事实
Payment Allocation = reservation 权威
Settlement = open amount 权威变化
~~~

## 15. API

业务 API：

~~~text
POST /ap/payment-applications
GET  /ap/payment-applications/{fid}
GET  /ap/payment-applications
GET  /ap/payment-applications/payables/{payableId}

POST /ap/payment-applications/{fid}/evidence
POST /ap/payment-applications/{fid}/evidence/{evidenceId}/verify
POST /ap/payment-applications/{fid}/budget-check
POST /ap/payment-applications/{fid}/submit
POST /ap/payment-applications/{fid}/approve
POST /ap/payment-applications/{fid}/reject
POST /ap/payment-applications/{fid}/cancel
~~~

内部 BOTP API：

~~~text
GET  /ap/internal/botp/payables/{fid}
GET  /ap/internal/botp/payment-applications/{fid}
GET  /ap/internal/botp/payment-applications/by-idempotency
POST /ap/internal/botp/payment-applications
POST /ap/internal/botp/payables/{fid}/recompute-reservation
~~~

## 16. 测试与 CI

测试：

~~~text
PaymentApplicationServiceTest
FiArapAdaptersTest
~~~

覆盖：

1. Formal AP open1000 创建申请600 → reserved=600，不减少 open。
2. evidence/budget 未通过阻断 submit。
3. reject 释放 reservation。
4. approve 保持 reservation 并追加 PAYMENT_APPLICATION_APPROVED。
5. legacy FI_AP_DOC BOTP 继续工作。
6. canonical FI_AP_PAYABLE 校验 available。
7. canonical target 使用 PA:{fid}。
8. BOTP writeback 重算 canonical reservation。

提交：

~~~text
7ad40d5d1ddb6b1f970b8ca1c5ca6613f27f0155
feat(fi): implement payment application reservation

87875528fb031e8c53263c320a08d4443cd6de3b
fix(botp): build canonical payment test context
~~~

CI：

~~~bash
mvn -q -Dstyle.color=never   -pl erp-service,fi-service,botp-service   -am test
~~~

成功 run：

~~~text
33031790230
Test ERP and FI business-finance chain → success
Upload Maven failure log             → skipped
~~~

## 17. 验收

- Formal AP 使用规范 `matrix_fi_ap_payable`。
- PaymentApplication 不回退到历史 `bizfi_fi_arap_doc`。
- 支持多 AP Allocation 与同 AP 多次申请。
- reservation 有行锁并发保护。
- DRAFT 创建即占用。
- Reject / Cancel 释放占用。
- Reject 重提重新抢占当前余额。
- Evidence / Budget 是提交门槛。
- APPROVED 不改变 AP open amount。
- APPROVED 不生成付款凭证。
- APPROVED 与 Business Event Outbox 同事务。
- Canonical / legacy BOTP 并存。
- Cancel 通知 BOTP Relation 生命周期。
- FI / ERP / BOTP P0 CI 全绿。
- matrix-prp 无修改。

## 18. 下一阶段

P0-IMP-07：

~~~text
PAYMENT_APPLICATION_APPROVED
→ PaymentOrder
→ Treasury Review
→ Payment Instruction
→ PAYING
→ BankTransaction
→ Reconciliation
→ PAID
~~~

`PaymentOrder AUDITED != PAID`；银行结果/回单事实才允许进入 PAID。
