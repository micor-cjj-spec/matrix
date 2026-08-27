
# P0-IMP-04 SupplierInvoice + 3-Way Match 实现记录

> 状态：Implemented v1
> 日期：2026-08-27
> 目标分支：dev

## 1. 目标与边界

本阶段实现：

~~~text
PurchaseOrder
→ PurchaseInbound
→ SupplierInvoice
→ P2P_3WAY_MATCH
→ MATCHED
→ SupplierInvoice Audit
→ SUPPLIER_INVOICE_CONFIRMED
~~~

本阶段不创建 Formal AP。正式应付、暂估冲回与正式应付会计事件属于 P0-IMP-05。

服务归属：

- erp-service：SupplierInvoice、生命周期、PO/Inbound 快照、审核时并发占用、Business Event Outbox。
- fi-service：P2P_3WAY_MATCH、Reconciliation Batch/Case/Participant/Match/Difference/Resolution、匹配规则与幂等。

继续保持 Relation ≠ Reconciliation ≠ Settlement ≠ Accounting。

## 2. 数据库对象

matrix_erp：

~~~text
matrix_erp_supplier_invoice
matrix_erp_supplier_invoice_entry
~~~

DDL：deliverables/erp/003-supplier-invoice/schema.sql

matrix_fi：

~~~text
matrix_fi_reconciliation_rule
matrix_fi_reconciliation_rule_version
matrix_fi_reconciliation_rule_field
matrix_fi_reconciliation_batch
matrix_fi_reconciliation_case
matrix_fi_reconciliation_participant
matrix_fi_reconciliation_match
matrix_fi_reconciliation_difference
matrix_fi_reconciliation_resolution
~~~

DDL + P0 V1 规则：deliverables/fi/004-p2p-three-way-match/schema.sql

规则代码：P2P_3WAY_MATCH。

P0 默认：

~~~text
tolerance = ZERO
partialInvoiceAllowed = true
blockingDifferences = true
~~~

## 3. SupplierInvoice 生命周期

主状态：

~~~text
DRAFT
→ SUBMITTED
→ ACCOUNTING_READY

异常：
REJECTED / CANCELLED
~~~

审批状态：

~~~text
DRAFT → SUBMITTED → AUDITED / REJECTED
~~~

匹配状态：

~~~text
UNMATCHED → MATCHING → MATCHED
                     ↘ PARTIAL_MATCHED
                     ↘ DIFFERENCE
                     ↘ UNMATCHED
~~~

只有 approvalStatus=AUDITED 前置条件满足且 matchStatus=MATCHED 的发票才允许最终审核进入 ACCOUNTING_READY。

## 4. SupplierInvoice API

根路径：

~~~text
/procurement/supplier-invoices
~~~

接口：

~~~text
POST   /procurement/supplier-invoices
PUT    /procurement/supplier-invoices/{fid}
GET    /procurement/supplier-invoices/{fid}
GET    /procurement/supplier-invoices

POST   /procurement/supplier-invoices/{fid}/submit
POST   /procurement/supplier-invoices/{fid}/match
POST   /procurement/supplier-invoices/{fid}/audit
POST   /procurement/supplier-invoices/{fid}/reject
POST   /procurement/supplier-invoices/{fid}/cancel
~~~

## 5. ERP → FI 内部三单匹配

Feign Client：

~~~text
single.cjj.erp.integration.fi.FiReconciliationClient
~~~

内部接口：

~~~text
POST /internal/reconciliation/p2p/three-way-match
X-Internal-Token: <token>
~~~

ERP 直接传不可变快照：

~~~text
InvoiceSnapshot
PurchaseOrderSnapshot
InboundSnapshot[]
~~~

FI 不反查 ERP 数据库。Participant 保存快照，因此历史对账结果不会因为来源单后续变化而失去解释能力。

## 6. 匹配口径

参与对象：

~~~text
PurchaseOrder Entry
PurchaseInbound Entry 1..N
SupplierInvoice Entry
~~~

P0 V1 检查：

~~~text
BusinessPartner
Currency
Material Id / Code
Specification
Confirmed Inbound Quantity
Unit Price
Net Amount
Tax Rate
Tax Amount
Gross Amount
~~~

差异类型：

~~~text
MISSING_DOCUMENT
PARTNER_DIFFERENCE
CURRENCY_DIFFERENCE
MATERIAL_DIFFERENCE
SPECIFICATION_DIFFERENCE
QUANTITY_DIFFERENCE
PRICE_DIFFERENCE
AMOUNT_DIFFERENCE
TAX_DIFFERENCE
~~~

P0 所有差异均为 BLOCKING，不自动批准差异。

## 7. 部分开票

允许正常部分开票：

~~~text
PO Qty       100
Inbound Qty  100
Invoiced Qty   0
Invoice Qty   60

Available = 100
Result = MATCHED
~~~

审核后 PO.finvoicedQuantity=60，PO.finvoiceStatus=PARTIAL。

第二张发票 40 再匹配并审核后，累计开票数量为 100，状态转 COMPLETE。

超量示例：

~~~text
Inbound 100
Already Invoiced 80
Current Invoice 30
Available 20

→ QUANTITY_DIFFERENCE
~~~

## 8. 并发控制

Match 只判定当前快照，不占用数量。

真正占用发生在 SupplierInvoice audit 的 ERP 本地事务：

~~~text
lock SupplierInvoice
lock PurchaseOrder headers，按 id 排序
lock PurchaseOrder entries，按 id 排序
re-check:
  finvoicedQuantity + currentInvoiceQuantity <= finboundQuantity
update finvoicedQuantity
refresh PO invoiceStatus
audit SupplierInvoice
append SUPPLIER_INVOICE_CONFIRMED outbox
commit
~~~

因此即使两个发票同时基于旧快照得到 MATCHED，也不能在审核阶段造成超开票。

## 9. Match 事务边界

远程 FI 调用不能被 ERP 长事务包住。

实现使用 TransactionTemplate：

~~~text
短事务 1
Invoice.matchStatus = MATCHING
commit

构建 ERP Snapshot
→ Feign 调 FI Reconciliation

短事务 2
保存 batchId / caseId / result / differenceCodes
commit
~~~

调用或快照构建失败时，通过独立短事务恢复 MATCHING → UNMATCHED。

这避免同类 self-invocation 导致 protected @Transactional 实际不生效。

## 10. Reconciliation 幂等

ERP requestId 基于“发票 + PO + confirmed Inbound”完整匹配快照的 SHA-256：

~~~text
P2P3WAY:{supplierInvoiceId}:{snapshotHashPrefix}
~~~

因此同一业务快照网络重试仍命中同一 Batch；PO / Inbound / Invoice 任一参与快照变化都会生成新的 requestId，允许重新匹配，同时避免“FI 已提交、ERP 响应丢失”造成重复 Batch。

ERP 还会校验迟到响应的 response.requestId 必须等于当前 fmatchRequestId，旧请求不能覆盖新匹配结果。

FI 唯一约束：

~~~text
(ftenant_id, frequest_id, fdelete_flag)
~~~

相同 requestId 已完成时直接返回原 Batch / Case / Difference；正在处理中时拒绝并发重复执行。

## 11. Business Event

审核通过与 Outbox 同一 ERP 本地事务发布：

~~~text
SUPPLIER_INVOICE_CONFIRMED
~~~

Payload 包含发票、客商、币种、净额/税额/含税额、reconciliationBatchId，以及每行 PO/物料/数量/价格/税/项目/成本中心/reconciliationCaseId 快照。

P0-IMP-04 只产生该事件，不消费生成 Formal AP。

## 12. 配置

ERP：

~~~yaml
erp:
  fi-reconciliation:
    base-url: ${ERP_FI_RECONCILIATION_BASE_URL:http://127.0.0.1:10003/api}
    internal-token: ${FI_RECONCILIATION_INTERNAL_TOKEN:change-me-before-production}
~~~

FI：

~~~yaml
fi:
  reconciliation:
    internal-token: ${FI_RECONCILIATION_INTERNAL_TOKEN:change-me-before-production}
~~~

生产环境必须覆盖默认 token。

## 13. 测试与验收

新增 P2pThreeWayMatchPolicyTest，覆盖：

1. 入库 100、发票 60 → MATCHED。
2. 入库 100、已开 80、再开 30 → QUANTITY_DIFFERENCE。
3. 单价变化 → PRICE_DIFFERENCE，且 BLOCKING。
4. 无 confirmed inbound → UNMATCHED。

CI：

~~~bash
mvn -q -Dstyle.color=never -pl erp-service,fi-service -am test
~~~

验收必须满足：

- SupplierInvoice CRUD/lifecycle 编译通过。
- Submitted 发票可执行 3-Way Match。
- 部分开票正常 MATCHED。
- 超量/价格/金额/税/客商/币种/物料差异持久化。
- Reconciliation Batch/Case/Participant/Match/Difference 落库。
- requestId 幂等。
- 未 MATCHED 不允许审核。
- audit 再次锁 PO 行检查，阻断并发超开票。
- audit 与 SUPPLIER_INVOICE_CONFIRMED Outbox 同事务。
- matrix-prp 无修改。

## 14. 下一阶段

P0-IMP-05：

~~~text
SUPPLIER_INVOICE_CONFIRMED
→ FI Inbox
→ Find AP Estimate
→ PURCHASE_ESTIMATE_REVERSAL
→ Formal AP
→ PURCHASE_AP_RECOGNITION
→ Accounting Rule
→ Voucher / Trace
~~~

暂估冲回必须基于原核算结果反向生成，不能用当前最新规则重新计算。
