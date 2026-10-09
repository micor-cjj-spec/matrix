# P0-05 Accounting Event → Voucher → GL 设计 v1

> 状态：Draft v1  
> 归档日期：2026-08-26

## 1. 目标

P0-05 把 P0-04 已经计算完成的 `AccountingResult` 安全落成凭证草稿，并经过复核、审核、过账进入总账，同时保持业务源单、业务事件、会计事件、规则版本、凭证和总账分录之间的完整追溯链。

目标链路：

```text
Business Event
    ↓
Accounting Event
    ↓
Accounting Rule
    ↓
Accounting Result READY
    ↓
Voucher Generation
    ↓
Voucher Draft / Submitted
    ↓
Review
    ↓
Audit
    ↓
Post
    ↓
GL Entry
```

## 2. 当前实现与主要缺口

Matrix 当前已经具备：

- `BizfiFiVoucher` 凭证主表实体。
- `BizfiFiVoucherLine` 凭证明细。
- `BizfiFiGlEntry` 过账总账分录。
- DRAFT → SUBMITTED → AUDITED → POSTED 基础生命周期。
- `source_request_id` 外部写入幂等字段。
- 已过账凭证冲销能力。

但存在以下缺口：

1. AR/AP 当前仍在 `BizfiFiArapDocServiceImpl` 内通过 `DOC_VOUCHER_ACCOUNT_MAP` 固定科目并直接创建凭证，绕过 Accounting Event / Rule。
2. 当前凭证明细没有项目、部门、成本中心、客户、供应商等动态辅助核算维度。
3. 当前 Voucher 生命周期缺少财务核算方案要求的“复核”步骤。
4. 当前凭证与 Accounting Event / Rule Version 没有结构化强关联。
5. 当前总账分录没有核算维度快照。
6. 当前过账实现通过删除已有 GL Entry 后重建实现幂等，不适合作为正式不可变账簿模型。

## 3. 核心边界

### 3.1 Accounting Engine 决定会计处理

业务模块、AR/AP 模块不得再直接决定借贷科目。

错误方向：

```text
AP Service
  ↓
if AP_ESTIMATE
  debit=1405
  credit=2202
  ↓
Voucher
```

目标方向：

```text
Business Event
  ↓
Accounting Event
  ↓
Accounting Rule
  ↓
Accounting Result
  ↓
VoucherGenerationService
```

### 3.2 Voucher 不重新计算会计规则

Voucher Generation 只消费 Accounting Result 中已经解析完成的：

```text
accountCode
amount
currency
summary
dimensions
cashflow
```

不在 Voucher Service 里再次进行科目匹配或业务判断。

## 4. Voucher Source Type

新增凭证来源语义：

```text
MANUAL
ACCOUNTING_EVENT
OPENAPI
IMPORT
OCR
SYSTEM_CLOSE
ADJUSTMENT
REVERSAL
```

历史 `BizfiFiVoucher` 保持兼容，建议增量增加：

```text
fsource_type
faccounting_event_id
foriginal_voucher_id
freversal_voucher_id
freviewed_by
freviewed_time
```

`source_request_id` 继续作为幂等键使用，不重新造一套重复字段。

自动核算凭证的 `source_request_id` 统一生成：

```text
ACCOUNTING:{accountingEventId}:VOUCHER:1
```

## 5. Voucher Generation Service

新增：

```text
single.cjj.fi.accounting.voucher
├─ AccountingVoucherService
├─ AccountingVoucherAssembler
├─ VoucherGenerationPolicy
└─ AccountingVoucherTraceService
```

核心接口：

```java
VoucherGenerationResult generate(String accountingEventId);
```

执行顺序：

```text
1. 锁定 Accounting Event
2. 校验状态必须是 READY / VOUCHER_GENERATED
3. 构造 sourceRequestId
4. 按 sourceRequestId 查询已有凭证
5. 已存在则幂等返回
6. 创建 Voucher
7. 创建 Voucher Lines
8. 创建 Voucher Line Dimensions
9. 写 Accounting Trace
10. 更新 Accounting Event = VOUCHER_GENERATED
```

步骤 6~10 必须位于同一个 `matrix_fi` 本地事务中。

## 6. Accounting Event 与 Voucher 的幂等

三层幂等链：

```text
FI Inbox
consumerCode + businessEventId
        ↓
Accounting Event
businessEventId + accountingEventType + sequence
        ↓
Voucher
sourceRequestId
```

即使 MQ、Inbox Worker 或 Voucher Generator 重试，都只能得到同一张凭证。

## 7. 自动凭证是否自动提交

增加策略：

```text
DRAFT_ONLY
AUTO_SUBMIT
```

P0 默认：

```text
DRAFT_ONLY
```

原因：先保证核算规则、辅助维度和凭证链路稳定，再逐步提升自动化程度。

P1/P2 稳定后可按 Accounting Rule 配置 `AUTO_SUBMIT`，但 P0 不允许自动复核、自动审核、自动过账。

## 8. Voucher 生命周期增加复核

目标状态机：

```text
DRAFT
  ↓ submit
SUBMITTED
  ↓ review
REVIEWED
  ↓ audit
AUDITED
  ↓ post
POSTED
```

驳回：

```text
SUBMITTED → REJECTED
REVIEWED  → REJECTED
```

冲销：

```text
POSTED
  ↓ reverse
REVERSED
```

规则：

- 复核人与制单人必须不同。
- 审核由权限/角色控制。
- POSTED 后凭证、分录和维度不可修改。
- Accounting Event 来源凭证默认禁止直接修改科目、借贷金额和核算维度。
- 需要人工调整时建立 `ADJUSTMENT` 凭证，不覆盖原自动核算结果。

## 9. 辅助核算维度

新增：

```text
matrix_fi_voucher_line_dimension
```

核心字段：

```text
fid
ftenant_id
forg_id
fvoucher_id
fvoucher_line_id
fdimension_code
fdimension_value_id
fdimension_value_code
fdimension_value_name
fsource_type
fcreate_time
fdelete_flag
fversion
```

唯一约束：

```text
voucher_line_id + dimension_code
```

示例：

```text
VoucherLine 1
  PROJECT       = P20260001
  COST_CENTER   = CC001

VoucherLine 2
  BUSINESS_PARTNER = S00001
```

维度同时保存 ID + Code + Name：

- ID：权威关联。
- Code/Name：历史快照。

## 10. GL 辅助核算维度

新增：

```text
matrix_fi_gl_entry_dimension
```

过账时把 Voucher Line Dimension 复制成不可变 GL Dimension Snapshot。

原因：

- 总账和辅助账不能依赖主数据当前名称。
- 维度余额查询需要稳定、可索引的数据结构。
- 凭证和总账都需要独立历史快照。

核心字段：

```text
fid
ftenant_id
forg_id
fgl_entry_id
fvoucher_id
fvoucher_line_id
fdimension_code
fdimension_value_id
fdimension_value_code
fdimension_value_name
fcreate_time
```

## 11. GL Entry 增强

现有 `bizfi_fi_gl_entry` 为历史表，先增量兼容，不在 P0 强制迁表。

建议补充：

```text
ftenant_id
forg_id
fbook_id
fperiod
fcurrency
foriginal_amount
frate
```

并增加唯一约束：

```text
fvoucher_id + fvoucher_line_id
```

## 12. 过账模型修正

当前实现会在过账前删除该凭证已有 GL Entry，再重建。

正式模型改为：

```text
AUDITED
  ↓
检查是否已经 POSTED
  ↓
按 Voucher Line 创建 GL Entry
  ↓
复制 Dimension Snapshot
  ↓
更新 Voucher = POSTED
```

原则：

- 已过账账簿记录不可删除再重建。
- 过账事务失败则整体回滚。
- 重复调用 POST 时，如果已 POSTED，直接幂等返回原结果。
- 数据库唯一索引阻止同一凭证行重复生成 GL Entry。

## 13. Accounting Trace

继续使用 P0-04 的：

```text
matrix_fi_accounting_trace
```

Voucher 生成后补充：

```text
accounting_event_id
rule_code
rule_version
voucher_id
voucher_line_id
```

过账后继续关联：

```text
gl_entry_id
```

最终支持：

```text
Business Document
→ Business Event
→ Accounting Event
→ Rule Version
→ Voucher
→ Voucher Line
→ GL Entry
```

反向也能从 GL 穿透回业务源单。

## 14. 冲销模型

冲销不能重新使用当前最新版 Accounting Rule 计算。

正确过程：

```text
Original Accounting Event
      ↓
Original Accounting Result
      ↓
Original Voucher / Lines / Dimensions
      ↓
反向借贷
      ↓
Reversal Voucher
```

结构化记录：

```text
originalAccountingEventId
originalVoucherId
reversalAccountingEventId
reversalVoucherId
```

不能只通过备注文本保存“冲销原凭证ID”。

## 15. OpenAPI 边界

现有 OpenAPI V3 继续保留“外部应用可靠创建凭证草稿”能力。

它属于：

```text
sourceType = OPENAPI
```

不强制走 Accounting Event，因为外部应用已经提交完整凭证明细。

但 OpenAPI 仍只允许创建 Draft，不开放提交、复核、审核、过账和冲销权限。

## 16. AR/AP 兼容迁移

当前：

```text
BizfiFiArapDocServiceImpl.generateVoucher()
  ↓
DOC_VOUCHER_ACCOUNT_MAP
  ↓
直接 insert Voucher
```

迁移目标：

```text
AR/AP Audit
  ↓
Business / Accounting Event
  ↓
Accounting Engine
  ↓
AccountingVoucherService
```

过渡期：

1. 旧 `generateVoucher` API 保持可调用。
2. 新 P2P 单据优先走 Accounting Event。
3. 旧硬编码映射标记 Deprecated。
4. Accounting Engine 覆盖稳定后删除 `DOC_VOUCHER_ACCOUNT_MAP`。

## 17. 第一个 E2E 用例

输入：

```text
PURCHASE_RECEIPT_CONFIRMED
GR2026080001
金额 10,000
项目 P001
成本中心 CC01
供应商 S001
```

P0-04：

```text
AE001
PURCHASE_RECEIPT_ESTIMATE_RECOGNITION
status = READY
```

Accounting Result：

```text
Line1 DEBIT
resolvedAccount = X
amount = 10,000
PROJECT = P001
COST_CENTER = CC01

Line2 CREDIT
resolvedAccount = Y
amount = 10,000
BUSINESS_PARTNER = S001
```

P0-05：

```text
Voucher V001
sourceType = ACCOUNTING_EVENT
accountingEventId = AE001
status = DRAFT
```

之后：

```text
DRAFT
→ SUBMITTED
→ REVIEWED
→ AUDITED
→ POSTED
```

最终：

```text
GL Entry 1 + Dimensions
GL Entry 2 + Dimensions
```

## 18. 验收标准

必须至少验证：

1. READY Accounting Event 只能生成一张凭证。
2. 重复执行 Voucher Generation 幂等返回同一 Voucher。
3. 借贷金额、币种、摘要、维度与 Accounting Result 完全一致。
4. 自动凭证能穿透到 Accounting Event 和 Rule Version。
5. Reviewer 不能等于 Creator。
6. 未 REVIEWED 不允许 AUDIT。
7. 未 AUDITED 不允许 POST。
8. POSTED 后凭证不可编辑。
9. 重复 POST 不产生重复 GL Entry。
10. GL Entry 可以查询对应辅助核算维度。
11. 冲销基于原凭证结果，不使用当前 Accounting Rule。
12. AR/AP 新流程不再通过硬编码科目直接创建凭证。

## 19. 与后续阶段关系

完成 P0-05 后链路为：

```text
Business Document
→ BOTP
→ Business Event
→ Accounting Event
→ Accounting Rule
→ Voucher
→ Review / Audit
→ GL
```

下一阶段 P0-06 建设统一 Reconciliation Framework，用于三单匹配、应付付款、应收收款、银行流水和各子账/总账之间的一致性校验。
