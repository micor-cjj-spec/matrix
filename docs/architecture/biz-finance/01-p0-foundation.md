# P0 业财一体基础底座设计 v1

> 状态：Draft v1  
> 归档日期：2026-08-26

## 1. P0 目标

P0 不直接铺开大量 ERP 页面，而是先建立后续 P2P、O2C、项目经营和财务核算都会复用的统一底座。

P0 核心链：

```text
BusinessPartner
      ↓
Business Document
      ↓
BOTP Conversion / Relation
      ↓
Business Event
      ↓
Transactional Outbox
      ↓
MQ
      ↓
FI Inbox
      ↓
Accounting Event
      ↓
Accounting Rule
      ↓
Voucher
      ↓
GL
```

## 2. 模块边界

### base-service

负责：

- 组织与基础资料
- BusinessPartner 统一主体
- Customer/Supplier 兼容 Facade

### erp-service

新建一个部署单元，先按领域包拆分：

```text
partner
crm
contract
sales
srm
procurement
inventory
manufacturing
project
```

### botp-service

继续作为单据转换基础设施：

- 单据类型注册
- 转换规则与版本
- 字段映射
- 执行幂等
- 单据上下游关系
- 分录级关系
- 状态失效与反写

### fi-service

保留现有 AR/AP/Fund/Expense/Asset/GL，并新增：

```text
accounting
├─ event
├─ rule
├─ resolver
├─ engine
└─ trace
```

## 3. P0-01 BusinessPartner

目标：统一客户和供应商的主体身份，不重复维护法人基础信息。

核心表：

```text
matrix_base_business_partner
matrix_base_business_partner_role
matrix_base_business_partner_org
matrix_base_business_partner_contact
matrix_base_business_partner_address
matrix_base_business_partner_bank_account
matrix_base_business_partner_tax_profile
matrix_base_business_partner_settlement
```

现有 `/customer/**`、`/supplier/**` 第一阶段保持兼容。

## 4. P0-02 BOTP Relation

目标：使现有 BOTP 真正支持 ERP 的复杂履约关系。

增强点：

- 使用完整 DocumentKey，不得只按 documentId 查询。
- Header Relation 增加关系语义。
- 正式启用 `matrix_botp_document_relation_entry`。
- 支持一对多、多对一、部分下推、数量/金额反写。
- 目标单作废后能够准确重算源单履约量。
- 增加 upstream/downstream/graph 穿透查询。

## 5. P0-03 Business Event

目标：建立业务系统到财务系统的可靠事件桥梁。

原则：

- Business Event 表达已经发生的业务事实，不表达命令。
- Outbox 必须与业务单据处于同一本地数据库事务。
- 不建设中央 Event Service。
- MQ 采用 at-least-once，消费侧通过 Inbox 保证幂等。
- 同一个业务事实重复消息是正常场景，消费者必须可重入。

示例：

```text
PURCHASE_RECEIPT_CONFIRMED
SUPPLIER_INVOICE_CONFIRMED
PAYMENT_COMPLETED
SALES_DELIVERY_CONFIRMED
```

## 6. P0-04 Accounting Event + Accounting Rule

目标：把业务事实转换成可审计、可版本化、可重放的会计处理结果。

```text
PURCHASE_RECEIPT_CONFIRMED
         ↓
PURCHASE_RECEIPT_ESTIMATE_RECOGNITION
         ↓
Accounting Rule
         ↓
Account Resolver
Amount Resolver
Dimension Resolver
         ↓
Accounting Result
```

P0-04 停在 `READY`，即规则计算完成、可以安全生成凭证，但暂不在本阶段改造完整 Voucher 工作流。

## 7. P0-05 Voucher/GL

待设计：

- Accounting Event → Voucher Draft
- Voucher 来源与幂等
- 辅助核算维度
- 复核状态
- 审核/过账策略
- 冲销凭证
- Voucher → GL
- 源单/事件/规则/凭证/总账穿透

## 8. P0-06 Reconciliation

待设计：

- ReconciliationTask
- ReconciliationRule
- ReconciliationMatch
- ReconciliationDifference

首批：

```text
PO ↔ Receipt ↔ Invoice
AP ↔ Payment
BankFlow ↔ Payment/Collection
```

## 9. P0-07 P2P E2E

最终验证：

```text
PO 100件
→ Receipt 60件
→ Receipt 40件
→ 再收1件必须失败
→ 作废第一张60件收货单
→ 已收40 / 剩余60 / PARTIAL
```

同时继续贯通：

```text
Receipt
→ Business Event
→ Accounting Event
→ Voucher
→ GL
```

## 10. 实施原则

1. 不重写现有 `fi-service`。
2. 不重建 BOTP 已有转换基础设施。
3. 新表必须遵守 Matrix 数据库命名规范。
4. 历史 `bizfi_*` 表按兼容方式逐步迁移。
5. 业务模块禁止直接硬编码生成凭证。
6. 规则采用版本化不可变发布，不允许历史核算结果被最新规则重算覆盖。
7. 先完成 P2P，再 O2C，再项目经营，制造域最后进入。
