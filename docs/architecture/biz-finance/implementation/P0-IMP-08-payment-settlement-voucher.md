# P0-IMP-08 Settlement + Payment Voucher 实现记录

> 状态：Implemented v1  
> 日期：2026-08-27  
> 目标分支：dev

## 1. 目标

~~~text
BANK_PAYMENT MATCHED
→ Payment Settlement Finalize
→ PaymentOrder PAID
→ AP Settlement
→ AP open / settled / reserved 更新
→ PaymentApplication Allocation consume
→ PaymentOrder Allocation CONSUMED
→ PAYMENT_COMPLETED Outbox
→ PURCHASE_PAYMENT_RECOGNITION
→ AccountingRuleEngine
→ Voucher
→ Accounting Trace
~~~

## 2. Finalize 事务边界

Finalize 采用显式接口，而不是在 Bank Match 中直接核销。

原因：BANK_PAYMENT=MATCHED 是稳定对账事实；Settlement 失败时可以重试，不破坏银行匹配结果。

Finalize 在同一 matrix_fi 本地事务内完成：

~~~text
lock PaymentOrder
lock matched BankTransaction
lock PaymentOrderAllocation
lock PaymentApplication
lock PaymentApplicationAllocation
lock AP Payable

→ insert Settlement
→ insert Settlement Entry
→ AP.settledAmount += amount
→ AP.openAmount -= amount
→ AP.reservedAmount -= amount
→ PaymentApplicationAllocation.consumedAmount += amount
→ PaymentOrderAllocation = CONSUMED
→ PaymentOrder = PAID
→ append PAYMENT_COMPLETED Outbox
→ commit
~~~

任一步失败整笔回滚。

## 3. Settlement

新增：

~~~text
matrix_fi_ap_settlement
matrix_fi_ap_settlement_entry
~~~

Settlement Header 绑定：PaymentOrder、BankTransaction、BusinessPartner、Currency、Amount。

Settlement Entry 穿透到：

~~~text
AP Payable
PaymentApplication
PaymentApplicationAllocation
PaymentOrderAllocation
~~~

并保存核销前后 open/reserved 快照。

## 4. 部分支付

PaymentApplicationAllocation 新增：

~~~text
fconsumed_amount
~~~

语义：

~~~text
reservedAmount = 原付款申请占用
consumedAmount = 已被成功支付核销金额
remainingReservedAmount = reservedAmount - consumedAmount
~~~

示例：

~~~text
AP1 reserved 400
AP2 reserved 600
PaymentOrder 600

AP1 consume 400 → allocation CONSUMED
AP2 consume 200 → allocation 仍 RESERVED，remaining 400
~~~

避免把部分支付误标为整条 reservation 已消费。

## 5. 重算修正

PaymentApplicationRepository.sumReservedByPayable 改为：

~~~text
SUM(reservedAmount - consumedAmount)
~~~

只统计 RESERVED allocation。

PaymentOrderRepository.sumOrderedByApplication 改为同时统计：

~~~text
ORDERED + CONSUMED
~~~

只有 RELEASED 才释放 orderedAmount。已支付 PaymentOrder 不会被重算丢失。

## 6. PaymentOrder PAID

P0-IMP-08 只有在以下条件全部满足时才允许 PAID：

~~~text
approvalStatus = AUDITED
bankMatchStatus = MATCHED
存在 matched + CONFIRMED BankTransaction
PaymentOrder Allocation 总额 = PaymentOrder.amount
AP reservation 足够
Settlement 总额 = PaymentOrder.amount
~~~

PAID 与 Settlement 在同事务内完成。

## 7. PAYMENT_COMPLETED

Finalize 成功后同事务写入 FI Transactional Outbox：

~~~text
eventType = PAYMENT_COMPLETED
routingKey = biz.finance.payment.completed
producerService = fi-service
sourceDocumentType = FI_PAYMENT_ORDER
~~~

payload 包含 PaymentOrder、Settlement、BankTransaction、BusinessPartner、BankAccount、Amount 和 Settlement Entries。

## 8. Payment Accounting

Business Event：

~~~text
PAYMENT_COMPLETED
~~~

Accounting Event：

~~~text
PURCHASE_PAYMENT_RECOGNITION
~~~

规则：

~~~text
DEBIT  FORMAL_AP
CREDIT BANK_DEPOSIT
~~~

不硬编码企业科目编码。FORMAL_AP / BANK_DEPOSIT 继续走现有 Account Mapping。

辅助核算：

~~~text
FORMAL_AP    → BUSINESS_PARTNER
BANK_DEPOSIT → BANK_ACCOUNT
~~~

Payment Accounting Consumer 默认关闭，待正式科目映射配置完成后启用；Queue/Binding 始终存在，避免事件不可路由。

## 9. Accounting 复用

本阶段没有新建第二套会计引擎，继续复用：

~~~text
matrix_fi_inbox_event
matrix_fi_accounting_event
AccountingRuleEngine
BizfiFiVoucherService
matrix_fi_accounting_trace
~~~

同一 consumerCode + eventId 幂等，重复 PAYMENT_COMPLETED 不重复出凭证。

## 10. Accounting Trace

Trace 增加：

~~~text
settlementId
paymentOrderId
bankTransactionId
~~~

形成：

~~~text
PaymentOrder
→ BankTransaction
→ Settlement
→ PAYMENT_COMPLETED
→ PURCHASE_PAYMENT_RECOGNITION
→ Voucher
~~~

## 11. API

~~~text
POST /fund/payment-settlements/payment-orders/{paymentOrderId}/finalize
GET  /fund/payment-settlements/{settlementId}
~~~

Finalize 重复调用时，如果 PaymentOrder 已存在 Settlement，直接返回原 Settlement。

## 12. 测试

新增：

~~~text
PaymentSettlementServiceTest
PaymentCompletedAccountingServiceTest
~~~

增强：

~~~text
AccountingRuleEngineTest
PaymentApplicationServiceTest
~~~

覆盖：

1. 400+600 reservation，支付600 → 400全消费 + 200部分消费。
2. AP open/settled/reserved 同步变化。
3. PaymentOrder Allocation → CONSUMED。
4. PaymentOrder 最后才进入 PAID。
5. finalize 重复调用幂等。
6. PAYMENT_COMPLETED → PURCHASE_PAYMENT_RECOGNITION。
7. 借 FORMAL_AP / 贷 BANK_DEPOSIT 平衡。
8. 重复 PAYMENT_COMPLETED 不重复出凭证。
9. orderedAmount 重算包含 CONSUMED。
10. reservedAmount 重算扣除 consumedAmount。

## 13. 提交与 CI

~~~text
a119382e2b3a7dbe3f1f848d375b524322aeb85c
feat(fi): finalize payment settlement and voucher chain
~~~

Biz Finance P0 CI：

~~~text
run 33037475206
Test ERP and FI business-finance chain → success
Upload Maven failure log             → skipped
~~~

执行：

~~~bash
mvn -q -Dstyle.color=never -pl erp-service,fi-service,botp-service -am test
~~~

## 14. 验收

- BANK_PAYMENT MATCHED 与 PAID 分离。
- Settlement 可显式、幂等 finalize。
- AP Settlement 与 PAID 同事务。
- AP open/settled/reserved 不变量成立。
- 部分消费 reservation 可表达。
- PaymentApplication ordered/reserved 重算不丢已支付金额。
- PAYMENT_COMPLETED 使用 Transactional Outbox。
- Payment Accounting 复用现有 AccountingRuleEngine/Voucher。
- 重复事件不重复出凭证。
- Accounting Trace 可穿透 PaymentOrder/Bank/Settlement/Voucher。
- Biz Finance P0 CI 全绿。
- matrix-prp 无修改。

## 15. 下一阶段

~~~text
P0-IMP-09 P2P frontend E2E
~~~

目标：把 Supplier → PO → Receipt → Acceptance → Inbound → Invoice → Match → AP → PaymentApplication → PaymentOrder → BankTransaction → Settlement → Voucher 的 P0 后端链路形成可操作、可追踪的前端工作台。