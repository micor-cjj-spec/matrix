# P0-IMP-07 PaymentOrder + BankTransaction 实现记录

> 状态：Implemented v1  
> 日期：2026-08-27  
> 目标分支：dev

## 1. 目标

~~~text
PaymentApplication APPROVED
→ PaymentOrder
→ Application orderedAmount
→ Submit
→ Liquidity Check
→ Audit
→ Submit to Bank
→ PAYING
→ BankTransaction
→ BANK_PAYMENT Reconciliation
→ bankMatchStatus = MATCHED
~~~

本阶段明确不执行 PAID / Settlement / Payment Voucher；这些统一留给 P0-IMP-08 的同一本地事务。

## 2. PaymentApplication 下推余额

matrix_fi_payment_application 新增 fordered_amount。

~~~text
amount = 已审批付款申请总金额
orderedAmount = 已被有效 PaymentOrder 占用的金额
availableOrderAmount = amount - orderedAmount
~~~

与 AP 余额分层：AP.openAmount、AP.reservedAmount、PaymentApplication.orderedAmount 三者不混用。

## 3. PaymentOrder

新增：

~~~text
matrix_fi_payment_order
matrix_fi_payment_order_allocation
~~~

状态：

~~~text
DRAFT → SUBMITTED → AUDITED → PAYING
SUBMITTED → REJECTED
DRAFT / REJECTED → CANCELLED
PAYING → FAILED
~~~

P0-IMP-07 明确 AUDITED != PAID、PAYING != PAID、BANK MATCHED != PAID。

## 4. orderedAmount 并发控制

创建 PaymentOrder 时按 paymentApplicationId 排序并 SELECT ... FOR UPDATE，校验 requested <= amount - orderedAmount 后更新 orderedAmount。

executionStatus：

~~~text
0        → NOT_EXECUTED
< amount → PARTIAL
= amount → COMPLETE
~~~

Reject / Cancel 会释放 orderedAmount；进入 AUDITED/PAYING 后不自动释放。

## 5. 执行控制

付款单提交前要求 paymentMethod、payerBankAccountId、payeeBankAccountId、payeeAccountName、payeeBankAccountNo 完整。

资金头寸校验：PENDING / PASSED / FAILED / NOT_REQUIRED。Audit 要求 PASSED 或 NOT_REQUIRED；PASSED 时 availableAmount >= PaymentOrder.amount。

## 6. BankTransaction

新增 matrix_fi_bank_transaction。自然幂等键：

~~~text
tenantId + bankAccountId + bankTransactionNo
~~~

记录 direction、currency、amount、counterpartyAccount、bankReceiptNo、sourceChannel、rawPayloadHash 等银行事实。

## 7. BANK_PAYMENT Reconciliation

复用统一 Reconciliation 表，并新增规则 BANK_PAYMENT_MATCH，scenario=BANK_PAYMENT。

P0 v1 比较：direction=OUTBOUND、付款银行账户、币种、金额、对手账号；金额容差 ZERO。

结果：MATCHED / DIFFERENCE。DIFFERENCE 写字段级 blocking difference。

## 8. MATCHED 事务边界

银行匹配成功只执行：

~~~text
BankTransaction.matchStatus = MATCHED
BankTransaction.matchedPaymentOrderId = PaymentOrder
PaymentOrder.bankMatchStatus = MATCHED
PaymentOrder.channelStatus = CONFIRMED
Reconciliation Batch / Case / Participant / Match
~~~

不执行 PaymentOrder.status = PAID。

P0-IMP-08 才执行：

~~~text
bankMatchStatus = MATCHED
→ PaymentOrder = PAID
→ AP Settlement
→ AP openAmount update
→ PaymentApplication reservation release
→ PAYMENT_COMPLETED
→ PURCHASE_PAYMENT_RECOGNITION
→ Payment Voucher
~~~

并要求同一 FI 本地事务。

## 9. BOTP

新规则 PAYMENT_APPLICATION_TO_PAYMENT_ORDER。

~~~text
source: MATRIX / FI_PAYMENT_APPLICATION / PA:{fid}
target: MATRIX / FI_PAYMENT_ORDER / PAYORD:{fid}
~~~

源校验：APPROVED + AUDITED，pushAmount > 0，且 pushAmount <= amount - orderedAmount。

目标创建成功后，BOTP Relation 保存并触发 source writeback，重算 PaymentApplication.orderedAmount。

## 10. Reverse Writeback 修正

检查取消补偿链时发现 BotpWritebackService 的异步/反向上下文缺少 tenantId。已补：

~~~text
context.put("tenantId", task.tenantId())
~~~

因此 PaymentOrder CANCELLED → BOTP Relation reverse → PaymentApplication orderedAmount recompute 可以在补偿任务中正确执行。

## 11. PaymentOrder Cancel

只有 DRAFT / REJECTED 可取消。取消后释放 orderedAmount，并发送：

~~~text
FI_PAYMENT_ORDER / PAYORD:{fid}
target-status CANCELLED
~~~

驱动 BOTP Relation 生命周期。REJECTED 不失效关系，因为允许重新提交。

## 12. API

PaymentOrder：

~~~text
POST /fund/payment-orders
GET  /fund/payment-orders/{fid}
GET  /fund/payment-orders
POST /fund/payment-orders/{fid}/liquidity-check
POST /fund/payment-orders/{fid}/submit
POST /fund/payment-orders/{fid}/audit
POST /fund/payment-orders/{fid}/reject
POST /fund/payment-orders/{fid}/cancel
POST /fund/payment-orders/{fid}/submit-to-bank
POST /fund/payment-orders/{fid}/channel-failure
~~~

BankTransaction：

~~~text
POST /fund/bank-transactions
GET  /fund/bank-transactions/{fid}
GET  /fund/bank-transactions
POST /fund/bank-transactions/{fid}/match-payment-order
~~~

## 13. 测试

新增 PaymentOrderServiceTest、BankPaymentMatchEvaluatorTest、BankTransactionServiceTest，并增强 FiArapAdaptersTest。

覆盖：orderedAmount 预占/释放、liquidity gate、银行精确匹配、blocking difference、MATCHED 不提前 PAID、三段 BOTP 链以及 reverse writeback tenant。

## 14. 提交与 CI

~~~text
89e21a62bc93759e9e120135f1c752175eb1ceee
feat(fi): implement payment order and bank matching

aff23d100274224d989e822258d8d5d15375cb54
fix(botp): import payment writeback command
~~~

第一次 CI 33032821709 因漏 import WritebackCommand 失败；修复后 run 33032896181 全绿。

~~~bash
mvn -q -Dstyle.color=never -pl erp-service,fi-service,botp-service -am test
~~~

## 15. 验收

- PaymentApplication → PaymentOrder 分层。
- orderedAmount 有行锁并发保护。
- reject/cancel 可释放 orderedAmount。
- submit/audit/pay-channel 状态分离。
- AUDITED/PAYING/MATCHED 均不等于 PAID。
- BankTransaction 自然幂等。
- BANK_PAYMENT 使用统一 Reconciliation。
- BOTP canonical 链完整。
- PaymentOrder cancel 可驱动 BOTP relation 失效/反写。
- reverse writeback 有 tenantId。
- ERP/FI/BOTP P0 CI 全绿。
- matrix-prp 无修改。

## 16. 下一阶段

~~~text
BANK_PAYMENT MATCHED
→ Payment Settlement Finalize
→ PaymentOrder PAID
→ AP Settlement
→ AP openAmount / settledAmount
→ release AP reservation
→ consume PaymentApplication / PaymentOrder allocations
→ PAYMENT_COMPLETED
→ PURCHASE_PAYMENT_RECOGNITION
→ Payment Voucher / Accounting Trace
~~~

所有余额和状态变化必须在同一 matrix_fi 本地事务内完成。