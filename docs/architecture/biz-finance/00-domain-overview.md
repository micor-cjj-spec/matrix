# Matrix 业财一体业务域总图 v1

> 状态：Draft v1  
> 归档日期：2026-08-26

## 1. 目标

Matrix 从现有财务平台扩展为 ERP / 业财一体平台时，不按页面或历史菜单拆模块，而按稳定业务域拆分，并围绕端到端业务链、主数据、业务单据、业务事件、财务结果建立统一模型。

业务需求基线明确覆盖线索到收款、采购到付款、生产到成本、申请到报销、基建项目、资产全生命周期、资金、税务、预算、人力等端到端流程，因此 Matrix 目标域需要同时覆盖经营、供应链、项目和财务。

## 2. 15 个目标业务域

1. 基础资料与组织 MDM
2. 客商与 CRM
3. 合同管理
4. 销售履约
5. SRM 供应商关系
6. 采购管理
7. 库存物流
8. 制造管理
9. 项目经营
10. 预算费控
11. 资金司库
12. 税务发票
13. 资产管理
14. 财务核算
15. 人力薪酬

## 3. 服务边界建议

```text
matrix
├─ base-service
│  └─ 企业级基础资料 / MDM / BusinessPartner
│
├─ erp-service
│  ├─ partner
│  ├─ crm
│  ├─ contract
│  ├─ sales
│  ├─ srm
│  ├─ procurement
│  ├─ inventory
│  ├─ manufacturing
│  └─ project
│
├─ botp-service
│  └─ 单据转换 / 单据关系 / 分录关系 / 反写
│
├─ fi-service
│  ├─ budget
│  ├─ expense
│  ├─ ar
│  ├─ ap
│  ├─ fund
│  ├─ tax
│  ├─ asset
│  ├─ cost
│  ├─ accounting
│  ├─ gl
│  └─ report
│
├─ workflow-service
├─ scheduler-service
├─ openapi-service
├─ im-service
├─ ai-service
└─ gateway
```

### 3.1 为什么先只增加一个 `erp-service`

不建议一开始把 CRM、采购、库存、销售、项目分别拆成独立微服务。

当前阶段采用：

> 先领域模块化，再微服务化。

原因：

- 当前 Matrix 规模尚不足以支撑大量独立服务的运维成本。
- P2P/O2C/项目之间需要高频协作，过早拆分会放大分布式事务和接口治理成本。
- 先在一个 `erp-service` 内保持领域包边界，未来按容量、团队和部署需求再拆服务。

## 4. 三层对象模型

### 4.1 经营/主数据对象

```text
Customer
Supplier
BusinessPartner
Material
Project
Employee
Asset
Organization
```

### 4.2 业务交易对象

```text
Lead
Opportunity
Contract
SalesOrder
PurchaseOrder
Receipt
Delivery
Invoice
Expense
PaymentRequest
Payment
Collection
```

### 4.3 财务结果对象

```text
Receivable
Payable
AccountingEvent
Voucher
Ledger
Cost
BudgetExecution
Tax
Report
```

统一转换关系：

```text
经营对象
  ↓
业务交易
  ↓
Business Event
  ↓
财务结果
```

## 5. 端到端链路

### 5.1 P2P

```text
Supplier
→ PurchaseRequest
→ PurchaseOrder
→ Receipt / Acceptance
→ SupplierInvoice
→ 3-Way Match
→ AP
→ PaymentRequest
→ Payment
→ BankFlow
→ Voucher
→ GL
```

### 5.2 O2C

```text
Lead
→ Opportunity
→ Quote / Tender
→ Contract
→ SalesOrder
→ Delivery / Acceptance
→ CustomerInvoice
→ AR
→ Collection
→ BankFlow
→ Voucher
→ GL
```

### 5.3 项目经营

```text
SalesContract
→ ProjectHandover
→ ProjectInitiation
→ ProjectPlan
→ BOM / MaterialDemand
→ Procurement / Issue / Expense
→ CostCollection
→ Acceptance
→ Revenue / Invoice / AR
→ Collection
```

## 6. 平台级横切能力

所有业务域共享以下基础设施：

```text
BusinessPartner
Document Conversion / Relation
Unified Status Convention
Business Event
Accounting Event
Rule
Reconciliation
Workflow
Scheduler
OpenAPI / Integration
Audit / Trace
AI
```

## 7. Business Partner

客户和供应商统一底层主体：

```text
BusinessPartner
  ├─ CUSTOMER
  ├─ SUPPLIER
  ├─ CUSTOMER + SUPPLIER
  ├─ FINANCIAL_INSTITUTION
  └─ OTHER
```

主体身份归主数据，CRM/SRM 只维护各自角色和业务属性。

## 8. 单据与财务的转换边界

```text
Business Document
      ↓
BOTP
Document Relation
      ↓
Business Event
      ↓
Accounting Event
      ↓
Accounting Rule
      ↓
Voucher
      ↓
Ledger
      ↓
Report
```

原则：

- BOTP 管“单据怎么转换、上下游怎么关联”。
- Business Event 管“业务发生了什么”。
- Accounting Event 管“该业务事实在财务上意味着什么”。
- Accounting Rule 管“该如何核算”。
- Voucher/GL 管“最终会计记录”。

## 9. 状态模型

一个单据不得用一个 `fstatus` 同时表达审批、履约、开票、结算和核算。

标准维度：

```text
fstatus                单据生命周期
fapproval_status       审批状态
fexecution_status      履约状态
fsettlement_status     结算状态
finvoice_status        开票状态
faccounting_status     核算状态
fclose_status          关闭状态
```

典型单据生命周期：

```text
DRAFT
→ SUBMITTED
→ EFFECTIVE
→ EXECUTING
→ COMPLETED
→ CLOSED
```

异常：

```text
REJECTED
CANCELLED
TERMINATED
```

## 10. Rule Center 方向

规则按领域拆分：

```text
ValidationRule
BudgetRule
CreditRule
PriceRule
AccountingRule
ReconciliationRule
CloseCheckRule
```

共享统一治理字段：

```text
code
version
status
effectiveDate
```

但 P0 不建设一个可执行任意脚本的“万能规则引擎”。

## 11. Reconciliation Center 方向

统一抽象：

```text
source
target
rule
difference
result
resolution
```

首批场景：

```text
PO ↔ Receipt ↔ Invoice
AP ↔ Payment
AR ↔ Collection
BankFlow ↔ Payment / Collection
```

后续扩展：

```text
Asset ↔ GL
Inventory ↔ GL
SubLedger ↔ GL
External ↔ Internal
```

## 12. 开发阶段

| 阶段 | 内容 |
|---|---|
| P0 | MDM、BusinessPartner、BOTP关系、Business Event、Accounting Event、Rule |
| P1 | P2P：SRM、采购、入库、发票、AP、付款 |
| P2 | O2C：CRM、合同、销售、发货、AR、收款 |
| P3 | Project：项目、预算、成本、采购、费用、收入 |
| P4 | Tax/Treasury/Credit/Reconciliation |
| P5 | BOM/MPS/生产/委外/实际成本 |
| P6 | AI Agent、经营分析、风险预警 |

制造域延后到 P5，避免在 P2P/O2C 尚未闭环前引入过重复杂度。
