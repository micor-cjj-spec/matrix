# P0-IMP-05 Formal AP + Estimate Reversal 实现记录

> 状态：Implemented v1  
> 日期：2026-08-27  
> 目标分支：`dev`

## 1. 目标

本阶段把已完成三单匹配并审核的供应商发票接入 FI：

~~~text
SUPPLIER_INVOICE_CONFIRMED
→ FI Inbox
→ Find Open AP Estimate
→ Full Estimate Reversal
→ Residual Estimate (if any)
→ Formal AP
→ PURCHASE_AP_RECOGNITION
→ Voucher / Trace
~~~

## 2. 财务语义

正式发票到达后，不允许在原 Estimate 上做“部分冲回后继续留余额”。

Matrix 采用：

~~~text
原 Estimate 100
发票 60

→ 原 Estimate 全额冲回 100
→ Formal AP 60 + tax
→ 未开票 40 重新形成 Residual Estimate
~~~

这样保留分批开票能力，同时保证原暂估本身被完整冲回。

Residual Estimate 是 Matrix 对分批开票场景的技术承接对象，不修改原 Estimate 的历史核算结果。

## 3. 核算快照原则

`PURCHASE_ESTIMATE_REVERSAL` 与
`PURCHASE_RESIDUAL_ESTIMATE_RECOGNITION`
不重新执行当前 Accounting Rule。

它们直接读取原 Estimate 的：

~~~text
Accounting Event
Accounting Event Entry
Accounting Event Dimension
resolved account
direction
amount
dimension snapshot
~~~

处理方式：

~~~text
Full Reversal
  原 DEBIT  → CREDIT
  原 CREDIT → DEBIT
  account / dimension 不变

Residual Estimate
  account / direction / dimension 沿用原会计快照
  amount 按未开票残余金额重建
~~~

因此历史暂估不会因为后续规则版本变化而被用新规则重新计算。

只有 Formal AP 使用当前：

~~~text
PURCHASE_AP_RECOGNITION
→ AccountingRuleEngine
~~~

## 4. 新增 FI 数据对象

DDL：

~~~text
deliverables/fi/005-formal-ap/schema.sql
~~~

新增：

~~~text
matrix_fi_ap_estimate_reversal
matrix_fi_ap_estimate_reversal_allocation
~~~

并增强：

~~~text
matrix_fi_accounting_rule_entry.fskip_zero_amount
matrix_fi_ap_payable.foriginal_payable_id

matrix_fi_ap_payable_entry:
  fnet_amount
  ftax_rate
  ftax_amount
  fgross_amount
~~~

### estimate_reversal

记录：

- SupplierInvoice Business Event
- 原 Estimate
- Formal AP
- 本次全额冲回金额
- Residual Estimate
- Residual Amount
- Reversal Accounting Event
- Residual Accounting Event

### reversal_allocation

记录：

~~~text
SupplierInvoiceEntry
→ EstimatePayableEntry
→ matched quantity
→ matched estimate amount
~~~

这张表表达“发票数量如何消费原暂估分录”，不改变“原 Estimate 整张冲回”的会计语义。

## 5. Estimate 分配

发票行以 PurchaseOrderEntry 为匹配键。

同一个 PO Entry 存在多张历史 Estimate 时：

~~~text
按 payable date
→ payable id
→ line no
→ entry id
FIFO
~~~

Planner：

~~~text
EstimateFullReversalPlanner
~~~

职责：

- Invoice quantity → Estimate Entry quantity 分配。
- 计算每个 Estimate Entry 已消费数量/金额。
- 找出所有被命中的 Estimate Payable。
- 数量不足时直接阻断。
- 最后一笔分配使用原分录剩余存储金额，避免小数舍入漂移。

## 6. Full Reversal + Residual

对于每个被命中的原 Estimate：

~~~text
lock Estimate
assert:
  type = ESTIMATE
  status = OPEN
  accountingStatus = VOUCHER_GENERATED
  openAmount = amount
~~~

然后：

~~~text
1. 读取原 Accounting Event Snapshot
2. 创建 PURCHASE_ESTIMATE_REVERSAL
3. 完整反向原核算结果
4. 生成 Reversal Voucher
5. 原 Estimate → REVERSED
6. 如果存在未开票残余：
   创建新的 ESTIMATE
   foriginal_payable_id = 原 Estimate
   创建 PURCHASE_RESIDUAL_ESTIMATE_RECOGNITION
   按原 Accounting Snapshot 生成 Residual Voucher
~~~

## 7. Formal AP

供应商发票生成：

~~~text
matrix_fi_ap_payable
ftype = FORMAL
fstatus = OPEN
fapproval_status = AUDITED
~~~

Formal AP Entry 保存：

~~~text
supplierInvoiceEntryId
purchaseOrderId
purchaseOrderEntryId
material
quantity
unitPrice
netAmount
taxRate
taxAmount
grossAmount
projectId
costCenterId
~~~

Formal AP 核算：

~~~text
PURCHASE_AP_RECOGNITION
~~~

兼容规则：

~~~text
Debit  PURCHASE_INVOICE_DEBIT = netAmount
Debit  INPUT_VAT              = taxAmount
Credit FORMAL_AP              = grossAmount
~~~

Matrix 已有兼容映射继续使用：

~~~text
PURCHASE_INVOICE_DEBIT → 1405
FORMAL_AP              → 2202
~~~

`INPUT_VAT` 不硬编码未知生产科目。

财务必须配置：

~~~text
faccount_key = INPUT_VAT
faccount_code = <财务确认的正式进项税科目>
~~~

再启用正式发票消费者。

## 8. 0% 税率

新增：

~~~text
matrix_fi_accounting_rule_entry.fskip_zero_amount
~~~

默认：

~~~text
0 = 金额必须 > 0
~~~

只有显式配置：

~~~text
skipZeroAmount = 1
~~~

的规则允许金额为 0 时跳过该分录。

P0 Formal AP 的 `INPUT_VAT` 行配置为 1。

因此：

~~~text
0% tax invoice
→ taxAmount = 0
→ skip INPUT_VAT line
→ 不要求解析 INPUT_VAT 科目

taxAmount > 0
→ 必须解析 INPUT_VAT
→ 未配置则明确失败
~~~

现有 P0-IMP-03 规则默认行为不变。

## 9. Business Event Routing

ERP 新增精确路由：

~~~text
SUPPLIER_INVOICE_CONFIRMED
→ biz.procurement.supplier_invoice.confirmed
~~~

并加入 ERP publisher required routing keys。

FI：

~~~text
queue:
matrix.fi.supplier-invoice-accounting

routing key:
biz.procurement.supplier_invoice.confirmed

dead queue:
matrix.fi.supplier-invoice-accounting.dead
~~~

## 10. 安全启用

当前默认：

~~~yaml
fi:
  accounting:
    supplier-invoice:
      enabled: false
~~~

环境变量：

~~~text
FI_SUPPLIER_INVOICE_ACCOUNTING_ENABLED
~~~

原因：

正式有税发票必须先配置财务确认的 `INPUT_VAT` account mapping。

Queue / Binding 可以先部署；Consumer 在条件开关为 true 时才启动消费。

## 11. 幂等与事务

Consumer 使用独立 consumer code：

~~~text
FI_SUPPLIER_INVOICE_ACCOUNTING_V1
~~~

沿用：

~~~text
matrix_fi_inbox_event
~~~

对：

~~~text
consumerCode + eventId
~~~

做幂等。

整个一次 SupplierInvoice accounting 在 FI 本地事务中完成：

~~~text
Inbox
→ lock Estimate
→ Formal AP
→ Reversal
→ Residual Estimate
→ Accounting Events
→ Voucher Drafts
→ Accounting Trace
→ Inbox PROCESSED
commit
~~~

异常整体回滚，由独立 FailureRecorder 记录 FAILED 并进入 DLQ，避免半完成的 AP / Voucher。

## 12. 测试

新增：

~~~text
EstimateFullReversalPlannerTest
EstimateSnapshotAccountingFactoryTest
AccountingRuleEngineTest#shouldSkipExplicitZeroAmountRuleEntry
~~~

覆盖：

1. Invoice 60 / Estimate 100：命中整张 Estimate，消费 60，Residual 40。
2. 一张发票跨多个 Estimate FIFO。
3. 分配最后一笔使用剩余金额避免 rounding drift。
4. Estimate 数量不足阻断。
5. Full Reversal 借贷方向完整反转，科目和维度不变。
6. Residual Recognition 继续使用原快照科目/维度。
7. 0 税规则行显式 skip。

## 13. CI

实现提交：

~~~text
2f812202df11ebe6a7dbf5361ce0c422aadaff11
feat(fi): implement formal AP and estimate reversal
~~~

Biz Finance P0 CI：

~~~text
run 33030444168
Test ERP and FI business-finance chain → success
Upload Maven failure log            → skipped
~~~

执行命令：

~~~bash
mvn -q -Dstyle.color=never -pl erp-service,fi-service -am test
~~~

## 14. 验收

P0-IMP-05 v1 满足：

- SupplierInvoice Event 精确路由。
- FI Inbox / DLQ 已接入。
- Formal AP 复用现有 AP 表。
- 原 Estimate 不允许部分状态冲回。
- 被命中的原 Estimate 全额反向原核算结果。
- Partial Invoice 会生成 Residual Estimate。
- Reversal / Residual 不调用最新 Accounting Rule。
- Formal AP 才使用当前 Accounting Rule。
- 税额 > 0 且未配置 INPUT_VAT 时明确失败。
- 0 税发票可显式跳过税分录。
- Accounting Trace 可追 SupplierInvoice → AP → Accounting Event → Voucher。
- 完整 ERP + FI P0 Maven CI 通过。
- matrix-prp 无修改。

## 15. 下一阶段

P0-IMP-06 PaymentApplication：

~~~text
Formal AP
→ PaymentApplication
→ budget / evidence / payable balance checks
→ approval
→ PAYMENT_APPLICATION_APPROVED
→ P0-IMP-07 PaymentOrder
~~~

PaymentApplication 不能直接等于付款指令；付款申请与实际支付执行继续分层。
