# Matrix 业财一体架构设计归档

> 状态：Draft v1  
> 归档日期：2026-08-26  
> 目标分支：`dev`

## 1. 文档定位

本目录归档 Matrix 从现有财务平台演进为 ERP / 业财一体平台的架构设计。

本目录不是对原始项目设计文档的复制，而是基于以下材料形成的 Matrix 目标设计：

1. `MHES-XXSJ-YCYT-业财一体项目详细设计方案1.0-20260817.docx`
   - 作为业务需求基线：场景、流程、角色、系统、输入输出单据、业务规则。
2. `MHES-XXSJ-CWHS-财务核算项目详细设计方案V6.0-20260626.docx`
   - 作为财务核算基线：核算场景、凭证、科目、辅助核算、总账、报表。
3. `MHES-XXSJ-YCYT-业财一体项目详细设计方案2.0-第七章-技术架构设计方案.docx`
   - 作为技术架构参考：集成、中间件、部署、安全、高可用。
4. Matrix `dev` 当前代码
   - 作为现状约束：`base-service`、`botp-service`、`fi-service`、`workflow-service`、`openapi-service`、`scheduler-service`、`im-service`、`ai-service` 等现有能力。

`matrix-prp` 仅作为只读参考，不在本设计归档过程中修改。

## 2. 来源标识约定

后续文档中结论分为四类：

- **业务需求依据**：来自业财一体 1.0。
- **财务规则依据**：来自财务核算 V6.0。
- **Matrix 当前实现**：来自 `matrix/dev` 现有代码。
- **Matrix 目标设计**：本轮针对 Matrix 作出的架构决策。

如原始材料没有给出具体表结构、接口、代码组织或技术细节，文档会明确将其标记为 Matrix 目标设计，不把推导结果冒充原文。

## 3. 总体设计原则

Matrix 的核心链路统一为：

```text
经营对象
  ↓
业务单据
  ↓
BOTP 单据转换与上下游关系
  ↓
业务状态确认
  ↓
Business Event
  ↓
Transactional Outbox / MQ / Inbox
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

横向控制链：

```text
Business Document / Finance Object
  ↓
Reconciliation Framework
  ↓
Difference
  ↓
Resolution
```

核心边界：

- `base-service`：组织、基础资料、主数据、Business Partner 主体。
- `erp-service`：CRM、合同、销售、SRM、采购、库存、制造、项目等经营域。
- `botp-service`：单据转换、单据上下游关系、分录关系、反写和转换幂等。
- `fi-service`：预算、费用、AR/AP、资金、税务、资产、成本、Accounting Event、Accounting Rule、Voucher、GL、Report、Reconciliation。
- 平台服务继续独立：workflow、scheduler、openapi、im、ai、gateway。

## 4. P0 设计目录

- [00-domain-overview.md](./00-domain-overview.md)：业财一体业务域总图。
- [01-p0-foundation.md](./01-p0-foundation.md)：P0 地基范围、模块边界和实施顺序。
- [02-business-partner.md](./02-business-partner.md)：统一客商主数据设计。
- [03-botp-document-relation.md](./03-botp-document-relation.md)：BOTP 单据转换与关系中心增强设计。
- [04-business-event.md](./04-business-event.md)：Business Event + Outbox/Inbox 设计。
- [05-accounting-event-rule.md](./05-accounting-event-rule.md)：Accounting Event + Accounting Rule 设计。
- [06-accounting-voucher-gl.md](./06-accounting-voucher-gl.md)：Accounting Event → Voucher → GL 设计。
- [07-reconciliation-framework.md](./07-reconciliation-framework.md)：统一对账 / 勾稽 / 差异处理框架。

架构决策记录：

- [ADR-001 Business Partner](./decisions/ADR-001-business-partner.md)
- [ADR-002 BOTP as Document Infrastructure](./decisions/ADR-002-botp-as-document-infrastructure.md)
- [ADR-003 Transactional Outbox](./decisions/ADR-003-transactional-outbox.md)
- [ADR-004 Accounting Event Model](./decisions/ADR-004-accounting-event-model.md)
- [ADR-005 Accounting Result to Voucher/GL](./decisions/ADR-005-accounting-result-to-voucher-gl.md)
- [ADR-006 Reconciliation Boundary](./decisions/ADR-006-reconciliation-boundary.md)

## 5. 当前 P0 进度

```text
P0-01 BusinessPartner                  已完成设计
P0-02 BOTP Document Relation           已完成设计
P0-03 Business Event                   已完成设计
P0-04 Accounting Event + Rule          已完成设计
P0-05 Accounting Event → Voucher → GL  已完成设计
P0-06 Reconciliation Framework         已完成设计
P0-07 P2P E2E 验证                     待设计/实现
```

## 6. 第一条端到端验证链

P0 最终以 P2P 链路作为第一条真实验证链：

```text
Supplier
→ PurchaseRequest
→ PurchaseOrder
→ Receipt
→ SupplierInvoice
→ 3-Way Match
→ AccountsPayable
→ PaymentRequest
→ Payment
→ BankFlow
→ Voucher
→ GL
```

第一阶段先验证：

```text
PurchaseOrder
→ BOTP
→ PurchaseReceipt
→ PURCHASE_RECEIPT_CONFIRMED
→ AccountingEvent
→ AccountingRule
→ Voucher Draft
→ Voucher Review / Audit
→ GL
```

同时验证控制链：

```text
PO + Receipt + SupplierInvoice
→ Reconciliation Batch
→ Reconciliation Case
→ MATCHED / PARTIAL_MATCHED / DIFFERENCE
→ Resolution
```

## 7. 数据库命名

所有新增数据库对象必须遵守 `docs/specs/database-naming-convention.md`：

- 数据库：`matrix_{module}`
- 表：`matrix_{module}_{business_table}`
- 字段：全部以 `f` 开头
- 主键：`fid`
- 通用字段：`ftenant_id`、`forg_id`、`fcreate_time`、`fmodify_time`、`fdelete_flag`、`fversion` 等

历史 `bizfi_*` 对象保留兼容，不作为新对象命名模板。
