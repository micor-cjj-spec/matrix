# P0-IMP-09 P2P Frontend E2E 实现记录

> 状态：Implemented v1  
> 日期：2026-08-27  
> Backend：matrix/dev  
> Frontend：matrix-web/dev

## 1. 目标

把 P0-IMP-01 ~ P0-IMP-08 的后端能力形成一个真实可操作、可追踪的 P2P 工作台，而不是继续使用旧 ARAP 占位页模拟采购与付款业务。

目标链：

```text
Supplier
→ PurchaseOrder
→ PurchaseReceipt
→ PurchaseAcceptance
→ PurchaseInbound
→ SupplierInvoice
→ Formal AP
→ PaymentApplication
→ PaymentOrder
→ BankTransaction
→ Payment Settlement
→ Voucher
```

## 2. Backend Query Support

为工作台补充两个只读查询能力：

```text
GET /ap/payment-applications/payables
GET /fund/payment-settlements
```

Formal AP 列表返回：

```text
amount
openAmount
settledAmount
reservedAmount
availableAmount
sourceDocument
accountingEventId
voucherId / voucherNumber
```

Settlement 列表返回 PaymentOrder、BankTransaction、BusinessPartner、Amount、BusinessEvent、AccountingEvent、Voucher。

后端支撑提交：

```text
e322a7f42cac4aaa3f8ca901db10363465311349
feat(fi): expose P2P payable and settlement queries
```

对应 CI：

```text
Biz Finance P0 CI      33037999894  success
Repository Hygiene CI 33037999908  success
Scheduler Reliability 33037999887  success
```

## 3. Frontend API Layer

新增：

```text
matrix-web/src/api/p2p.js
```

统一封装：

```text
PurchaseOrder
PurchaseReceipt
PurchaseAcceptance
PurchaseInbound
SupplierInvoice
FormalPayable
PaymentApplication
PaymentOrder
BankTransaction
PaymentSettlement
BOTP Execution
BOTP Relation
```

页面不直接散落 axios URL。

## 4. P2P Workbench

新增：

```text
src/views/login/p2p/P2pWorkbenchView.vue
route: /p2p
```

阶段：

```text
01 PurchaseOrder
02 PurchaseReceipt
03 PurchaseAcceptance
04 PurchaseInbound
05 SupplierInvoice
06 Formal AP
07 PaymentApplication
08 PaymentOrder
09 BankTransaction
10 Settlement
11 Voucher
```

供应商主数据入口保留并跳转现有 Supplier 页面。

## 5. Procurement Operations

工作台支持：

```text
PurchaseOrder
  create / submit / audit / reject / cancel

PurchaseOrder → PurchaseReceipt
  available = quantity - receiptReservedQuantity

PurchaseReceipt
  submit / confirm / reject / cancel

PurchaseReceipt → PurchaseAcceptance
  available = quantity - inspectionReservedQuantity

PurchaseAcceptance
  submit / confirm / reject / cancel

PurchaseAcceptance → PurchaseInbound
  available = qualified + concession - inboundReserved

PurchaseInbound
  submit / confirm / reject / cancel

PurchaseInbound → SupplierInvoice
  available = PO.inboundQuantity - PO.invoicedQuantity

SupplierInvoice
  submit → 3-way match → audit / reject / cancel
```

所有数量最终仍由后端领域服务校验；前端可用量只用于操作提示和预填，不作为权威业务规则。

## 6. Finance Operations

Formal AP：

```text
显示 open / settled / reserved / available
显示来源业务单、AccountingEvent、Voucher
```

PaymentApplication：

```text
Evidence
Budget Check
submit
approve / reject / cancel
```

PaymentOrder：

```text
Liquidity Check
submit
audit / reject / cancel
submit-to-bank
```

BankTransaction：

```text
create outbound transaction
BANK_PAYMENT match
```

Settlement：

```text
PaymentOrder + matched BankTransaction
→ finalize
→ PaymentOrder PAID
→ AP Settlement
→ PAYMENT_COMPLETED
→ payment voucher
```

## 7. BOTP

P0-IMP-09 v1 真实使用的内置 BOTP 规则：

```text
FORMAL_AP_TO_PAYMENT_APPLICATION
PAYMENT_APPLICATION_TO_PAYMENT_ORDER
```

工作台传递业务参数：

```text
pushAmount
payMethod
plannedPayDate
payerBankAccountId
operatorId
```

BOTP Execution 自动补齐：

```text
tenantId
executionId
sourceSystemCode
sourceDocumentType
sourceDocumentId
```

并继续使用 requestId + target idempotency key 防止重复创建。

## 8. BOTP Boundary / Known Gap

当前 `botp-service` 尚未提供采购履约段内置规则：

```text
PO → Receipt
Receipt → Acceptance
Acceptance → Inbound
Inbound → SupplierInvoice
```

因此 P0 前端 v1 在采购段调用对应领域 API 创建下游单据，由后端 reservation / quantity invariant 保证业务正确性。

不能把这部分描述为已通过 BOTP 转换。

现阶段 BOTP Relation 追踪重点覆盖：

```text
Formal AP
→ PaymentApplication
→ PaymentOrder
```

采购段 BOTP 化进入 P1，不在 P0-IMP-09 中重复造一套伪 Relation。

## 9. Navigation

财务云、应付模块和供应链云增加 P2P 入口。

新增/调整：

```text
/p2p
/payment-application → /p2p?stage=payment-application
/payment-processing  → /p2p?stage=payment-order
/settlement-processing → /p2p?stage=settlement
```

`src/main.js` legacyRedirect 同步修正，避免旧路由守卫覆盖新工作台。

`/estimated-payable` 保留暂估应付语义，不错误映射到 Formal AP。

## 10. Tenant / Org Context

P0 v1 暂未建立全平台统一业务上下文切换器。

工作台：

```text
优先读取 localStorage tenantId / orgId
允许顶部手工切换
持久化 p2pTenantId / p2pOrgId
```

后续统一 Organization Context 落地后再接管，不要求重写 P2P API。

## 11. CI

新增：

```text
.github/workflows/p2p-web-ci.yml
```

门禁：

```text
npm ci
npm run build
```

前端提交：

```text
24cb6691762b92853f9cd2673e39b2f81322908c
feat(p2p): add procure-to-pay E2E workbench
```

GitHub Actions：

```text
P2P Web CI          33044086637  success
IM Realtime Web CI  33044086640  success
Knowledge Web CI    33044086608  success
Scheduler Web CI    33044086611  success
```

P2P Vite build 首次通过，无编译修复 commit。

## 12. P0-IMP-09 Acceptance

- `/p2p` 可访问。
- 11 个阶段可按 tenant/org 查询。
- 采购阶段可以真实创建和流转业务单据。
- SupplierInvoice 可以执行三单匹配和审核。
- Formal AP 可显示余额、占用和会计追踪。
- Formal AP 可通过 BOTP 下推 PaymentApplication。
- PaymentApplication 支持 Evidence + Budget Gate。
- PaymentApplication 可通过 BOTP 下推 PaymentOrder。
- PaymentOrder 支持 Liquidity Gate 与支付渠道提交。
- BankTransaction 可录入并与 PaymentOrder 对账。
- Settlement 可以显式 finalize。
- Voucher 阶段和 Settlement Voucher 可进入现有凭证管理。
- BOTP Relation 可查询财务单据转换关系。
- legacyRedirect 不再把付款入口送回旧占位页。
- P2P Web CI 全绿。
- matrix-prp 无修改。

## 13. P0 Closure Status

P0 第一条 P2P 主链的代码级实现已闭环：

```text
Business document
→ Business Event
→ Reconciliation
→ AP
→ Payment
→ Bank fact
→ Settlement
→ Accounting Event
→ Voucher
```

但“代码级闭环”不等于生产可用验收完成。

仍需在部署环境执行真实 runtime smoke / integration test，尤其覆盖：

```text
异步 MQ 消费
Accounting consumer 开关与正式科目映射
数据库 migration
RabbitMQ routing
跨服务 gateway 路由
异常补偿 / reversal
真实组织与权限上下文
```

## 14. P1 Direction

按 P0-07 原计划，第一条链闭环后进入 P1：

```text
完整采购前置
SRM
合同
采购退货
采购异常 / 索赔 / 扣款
税务发票深化
采购履约 BOTP 化
```

不再增加新的平台级抽象，优先扩展真实业务覆盖。