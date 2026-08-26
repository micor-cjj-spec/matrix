# P0-04 Accounting Event + Accounting Rule 设计 v1

> 状态：Draft v1  
> 归档日期：2026-08-26

## 1. 目标

把 Business Event 转换为可审计、可版本化、可重放的会计处理结果：

```text
Business Event
→ Accounting Event
→ Accounting Rule
→ Account Resolver
→ Amount Resolver
→ Dimension Resolver
→ Accounting Result
→ READY
```

P0-04 暂停在 `READY`，凭证生成和完整 Voucher Workflow 放到 P0-05。

## 2. 业务/财务依据

采购入库确认后需要形成暂估应付；采购发票确认后形成正式应付；财务应付单审核后生成采购成本、应付、进项税相关凭证。

财务核算规则同时要求：收到发票后，对原暂估先全额冲回，再按发票金额确认正式应付；审核时需要校验科目和辅助核算维度。

完整 13 个核算域的具体账务规则由《MHES-CWHS-企业财务核算规则标准表V2.0-20260608》承载。该附件尚未纳入当前归档依据，因此本设计提供规则能力和取值机制，不擅自写死未被材料明确支持的最终科目编码。

## 3. Business Event 与 Accounting Event

```text
PURCHASE_RECEIPT_CONFIRMED
        ↓
PURCHASE_RECEIPT_ESTIMATE_RECOGNITION
        ↓
Accounting Rule
        ↓
Accounting Result
```

Business Event 表达业务事实；Accounting Event 表达该业务事实的会计含义。

## 4. 一对多

一个 Business Event 可以产生多个 Accounting Event。

典型：

```text
SUPPLIER_INVOICE_CONFIRMED
        ├→ PURCHASE_ESTIMATE_REVERSAL
        └→ PURCHASE_AP_RECOGNITION
```

冲回引用原 Accounting Event，并必须基于原核算结果快照完成，不允许用最新规则重算历史冲销。

## 5. Accounting Event 表

新增：

```text
matrix_fi_accounting_event
```

核心字段：

```text
fid
ftenant_id
forg_id
faccounting_org_id
faccounting_event_id
faccounting_event_type
fbusiness_event_id
fbusiness_event_type
fsequence_no
fbook_id
fsource_system_code
fsource_document_type
fsource_document_id
fsource_document_no
fbusiness_date
faccounting_date
fcorrelation_id
fcausation_id
foriginal_accounting_event_id
frule_code
frule_version
fstatus
fstage
ffacts_json
fsource_payload_hash
ferror_code
ferror_message
fcreate_time
fmodify_time
fdelete_flag
fversion
```

核心幂等键：

```text
tenant
+ businessEventId
+ accountingEventType
+ sequenceNo
```

## 6. 状态与阶段分离

`fstatus`：

```text
RECEIVED
PROCESSING
READY
VOUCHER_GENERATED
POSTED
REVERSED
FAILED
IGNORED
```

`fstage`：

```text
VALIDATION
RULE_MATCH
ACCOUNT_RESOLVE
DIMENSION_RESOLVE
AMOUNT_CALCULATION
VOUCHER_GENERATION
POSTING
REVERSAL
```

错误码单独保存，例如：

```text
ACCOUNTING_RULE_NOT_FOUND
ACCOUNTING_RULE_CONFLICT
ACCOUNT_NOT_RESOLVED
DIMENSION_MISSING
AMOUNT_INVALID
ACCOUNTING_UNBALANCED
```

## 7. Rule 主表 + 不可变版本

新增：

```text
matrix_fi_accounting_rule
matrix_fi_accounting_rule_version
matrix_fi_accounting_rule_entry
matrix_fi_accounting_rule_dimension
```

发布后的 RuleVersion 不允许修改。规则调整必须创建新版本、审核并发布。

示例：

```text
PURCHASE_RECEIPT_ESTIMATE V1
PURCHASE_RECEIPT_ESTIMATE V2
PURCHASE_RECEIPT_ESTIMATE V3
```

历史 Accounting Event 固定记录实际使用的 `ruleCode + ruleVersion`。

## 8. 唯一规则命中

Rule Matcher 按以下上下文筛选：

```text
eventType
accountingOrg
book
businessType
materialCategory
projectType
assetFlag
taxType
effectiveDate
priority
specificity
```

结果必须 deterministic：

```text
0条  → ACCOUNTING_RULE_NOT_FOUND
1条  → 正常
多条同优先级 → ACCOUNTING_RULE_CONFLICT
```

不得用 `rules.get(0)` 随机取第一条。

## 9. Account Resolver

P0 不强制每条规则直接写死科目编码，支持 `AccountKey`：

```text
PURCHASE_RECEIPT_DEBIT
ESTIMATED_AP
FORMAL_AP
INPUT_VAT
BANK_DEPOSIT
```

P0 第一版 Resolver：

```text
FIXED
MAPPING
```

MAPPING 根据：

```text
accountKey
+ accountingOrg
+ book
+ businessContext
→ accountCode
```

后续可扩展物料类别、费用类别、资产类别、项目类型等映射，但不允许在 P0 引入任意 Groovy/JavaScript/SpEL 执行。

## 10. Amount Resolver

只支持受控表达式：

```text
FIELD(payload.totalAmount)
FIELD(line.amount)
FIELD(line.taxAmount)
SUM(lines.amount)
ADD(a,b)
SUB(a,b)
NEGATE(a)
```

禁止 `eval` 任意脚本。

## 11. Rule Entry

`matrix_fi_accounting_rule_entry` 主要字段：

```text
frule_version_id
fline_no
fdirection              DEBIT/CREDIT
faccount_source_type    FIXED/MAPPING
faccount_key
faccount_code
famount_expression
fsummary_template
fcurrency_expression
fcashflow_item_key
fcondition_json
fgrouping_key
```

概念示例：

```text
Rule PURCHASE_RECEIPT_ESTIMATE

Entry1
DEBIT
accountKey=PURCHASE_RECEIPT_DEBIT
amount=line.amount

Entry2
CREDIT
accountKey=ESTIMATED_AP
amount=line.amount
```

实际科目由正式核算规则配置解析。

## 12. 辅助核算维度

VoucherLine 不增加固定 `project_id/customer_id/supplier_id/...` 字段，而采用动态行级维度表：

```text
matrix_fi_voucher_line_dimension
```

建议保存：

```text
fvoucher_line_id
fdimension_code
fdimension_value_id
fdimension_value_code
fdimension_value_name
```

ID 负责关联，Code/Name 保存历史快照。

规则维度定义：

```text
matrix_fi_accounting_rule_dimension
```

例如：

```text
PROJECT      ← line.projectId
COST_CENTER  ← line.costCenterId
BUSINESS_PARTNER ← payload.businessPartnerId
```

是否 required 由规则决定。

## 13. Accounting Event Entry

新增：

```text
matrix_fi_accounting_event_entry
matrix_fi_accounting_event_dimension
```

保存实际规则计算结果：

```text
faccounting_event_id
fline_no
fdirection
faccount_key
fresolved_account_code
fsummary
fdebit_amount
fcredit_amount
fcurrency_code
frate
foriginal_amount
frule_entry_id
```

生成凭证前强制：

```text
Debit Total = Credit Total
```

不平衡则 `ACCOUNTING_UNBALANCED`。

## 14. Accounting Trace

新增：

```text
matrix_fi_accounting_trace
```

连接：

```text
Business Event
→ Accounting Event
→ Rule + Version
→ Voucher
→ Voucher Line
→ GL Entry
```

可同时保存：

```text
source document
BOTP execution/relation
original accounting event
reversal accounting event
```

## 15. 三层幂等

```text
FI Inbox
consumerCode + businessEventId

Accounting Event
businessEventId + accountingEventType + sequence

Voucher
sourceRequestId = ACCOUNTING:{accountingEventId}:VOUCHER:{index}
```

Worker 重试不得生成重复凭证。

## 16. 当前 Voucher 兼容

现有 `BizfiFiVoucher`、`BizfiFiVoucherLine`、`BizfiFiGlEntry` 继续复用。

现状缺口：

- VoucherLine 尚无动态辅助核算维度。
- Voucher 状态目前是 `DRAFT → SUBMITTED → AUDITED → POSTED`，缺少财务方案要求的独立复核环节。
- 当前 reverse 主要通过备注关联原凭证，后续需改为结构化 Trace。

这些在 P0-05 处理，不在 P0-04 顺手重写凭证内核。

## 17. P2P 第一条核算链

输入：

```text
PURCHASE_RECEIPT_CONFIRMED
GR2026080001
Supplier S001
Amount 10000
Project P001
CostCenter CC01
```

生成：

```text
AE001
PURCHASE_RECEIPT_ESTIMATE_RECOGNITION
Rule PURCHASE_RECEIPT_ESTIMATE V1
```

概念结果：

```text
DEBIT
AccountKey = PURCHASE_RECEIPT_DEBIT
Amount = 10000
Dimensions = PROJECT P001, COST_CENTER CC01

CREDIT
AccountKey = ESTIMATED_AP
Amount = 10000
Dimensions = BUSINESS_PARTNER S001
```

借贷平衡后：

```text
status = READY
```

P0-05 再从 READY 进入 Voucher Draft / Review / Audit / Post / GL。
