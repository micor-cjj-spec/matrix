# P0-06 Reconciliation Framework 对账 / 勾稽中心设计 v1

> 状态：Draft v1  
> 归档日期：2026-08-26

## 1. 目标

P0-06 建立 Matrix 统一的对账 / 勾稽基础框架，用于判断两个或多个业务/财务对象之间的数据是否一致、差异在哪里、差异如何处理，并保留完整的规则版本、执行批次、参与对象、差异和人工处理轨迹。

第一阶段覆盖：

```text
PO ↔ Receipt ↔ Supplier Invoice       三单匹配
AP ↔ Payment                          应付核销前匹配
AR ↔ Collection                       应收收款核销前匹配
Bank Flow ↔ Payment / Collection      银行流水认领/对账
AR Subledger ↔ GL                     应收明细账与总账
AP Subledger ↔ GL                     应付明细账与总账
```

后续可扩展：

```text
Inventory ↔ GL
Asset ↔ GL
Expense ↔ Payment
Tax Invoice ↔ AP/AR
Internal AR ↔ Internal AP
Financial Statements ↔ GL
```

## 2. 需求依据

### 2.1 三单匹配

业财一体 1.0 明确三单为：采购订单、采购入库单、供应商增值税发票；需要核对货品规格、单价、实收数量、票面金额和品名，并由系统自动匹配、标记差异，差异至少包括数量、价格、入库批次。

### 2.2 应收核销与对账

财务核算 V6 要求：

- 收款与应收挂账核对客户、金额和对应订单。
- 收款核销金额不得超过应收余额。
- 应收余额 = 收入确认 - 收款核销 - 坏账核销 + 预收转应收。
- 应收明细账合计须与总账应收科目余额一致。
- 应收账款按月/季度定期对账。

### 2.3 应付核销与对账

财务核算 V6 要求：

- 应付付款金额不得超过应付余额。
- 预付款核销金额不得超过预付余额。
- 付款银行账户币种须与付款币种一致。
- 应付余额 = 暂估 + 挂账 - 付款 - 预付核销。
- 应付明细账合计须与总账应付科目余额一致。
- 应付账款按月/季度定期对账。

### 2.4 银行流水

财务核算方案要求资金模块对银行流水匹配业务单据形成收付款凭证，并在月末进行资金对账、形成银行余额调节表。

## 3. 与现有 Matrix 能力的关系

### 3.1 已有 AR/AP 自动核销

当前 `BizfiFiArapManageServiceImpl` 已具备一个局部匹配能力：

```text
AR:
SOURCE = AR / AR_ESTIMATE
TARGET = AR_SETTLEMENT

AP:
SOURCE = AP / AP_ESTIMATE
TARGET = AP_PAYMENT_PROCESS
```

当前自动核销：

```text
已审核单据
→ 按往来方分组
→ 最早源单优先匹配最早结算单
→ 生成 writeoff log
→ 生成 writeoff link
```

当前实现的优点：

- 已具备 partial amount 分配思想。
- 有核销批次和明细链接。
- 查询 open amount 时可以扣减已持久化的核销链接。

当前缺口：

- 只覆盖 AR/AP，不是统一框架。
- 匹配规则硬编码为 FIFO greedy。
- 只比较金额和往来方，不具备字段级差异模型。
- 当前自动核销只落日志和链接，不直接回写单据状态。
- 没有规则版本、任务快照和人工差异处理模型。

P0-06 不删除这套能力，而是把它逐步迁入统一 Reconciliation Framework。

### 3.2 OpenAPI Reconcile 不是业务对账中心

当前 `BizfiFiVoucherOpenApiReconcileController` 的职责只是：

```text
sourceRequestId
→ 查找是否已生成 Voucher
```

它用于 OpenAPI 异步写入后的技术对账，不等同于业务/财务对账中心，应继续保留原边界。

## 4. 三个概念必须分开

### 4.1 BOTP Relation

回答：

> 两张单据为什么有关联、由谁生成谁、履约了多少？

例如：

```text
PO.Line1 --FULFILLS--> Receipt.Line8
```

### 4.2 Reconciliation

回答：

> 相关数据是否一致、差异在哪？

例如：

```text
PO Qty       = 100
Receipt Qty  = 100
Invoice Qty  = 90

Result = DIFFERENCE
Difference = QUANTITY_DIFFERENCE
```

### 4.3 Write-off / Settlement

回答：

> 已确认的收付款应当冲销哪一笔应收/应付余额？

例如：

```text
AP 10000
Payment 6000
→ Write-off 6000
→ AP Open Amount 4000
```

核心原则：

> Relation ≠ Reconciliation ≠ Write-off。

Reconciliation 只判断和记录，不直接修改业务余额；真正的核销/结算由 AR/AP/Fund 等所属领域执行。

## 5. 部署边界

P0 不新建 `reconciliation-service`。

第一阶段放在：

```text
fi-service
└─ single.cjj.fi.reconciliation
   ├─ rule
   ├─ execution
   ├─ participant
   ├─ matcher
   ├─ difference
   ├─ resolution
   ├─ adapter
   └─ query
```

原因：

- 第一批场景全部与财务管控直接相关。
- 避免新增微服务及新的分布式事务边界。
- 以后若对账量、团队边界或部署需求足够大，再单独拆服务。

## 6. 核心对象模型

统一模型：

```text
ReconciliationRule
        ↓
ReconciliationBatch
        ↓
ReconciliationCase
        ├─ Participant 1..N
        ├─ Match Snapshot
        ├─ Difference 0..N
        └─ Resolution 0..N
```

### 6.1 Rule

定义：

- 对什么场景做对账。
- 参与方有哪些。
- 如何选择候选。
- 比较哪些字段。
- 允许多大容差。
- 哪些差异是阻断性差异。
- 是否允许自动确认。

### 6.2 Batch

一次实际执行：

```text
P2P_3WAY_MATCH
2026-08-26 15:00
Rule V3
AccountingOrg 100
```

### 6.3 Case

一个可独立判断的对账单元。

三单匹配建议以采购订单分录 / 匹配业务键为 Case：

```text
Case C001
PO.Line1
Receipt.Line10 + Receipt.Line11
Invoice.Line5
```

### 6.4 Participant

Case 的参与对象。

参与方角色示例：

```text
PURCHASE_ORDER
PURCHASE_RECEIPT
SUPPLIER_INVOICE
AR
COLLECTION
AP
PAYMENT
BANK_FLOW
SUBLEDGER
GL
```

Participant 必须保存：

```text
systemCode
documentType
documentId
documentNo
entryId
businessPartnerId
currency
businessDate
snapshotJson
```

保证未来源单变化时，历史对账仍可解释。

## 7. 数据库模型

所有新表位于 `matrix_fi`，遵循 Matrix 数据库命名规范。

第一版建议：

```text
matrix_fi_reconciliation_rule
matrix_fi_reconciliation_rule_version
matrix_fi_reconciliation_rule_field
matrix_fi_reconciliation_batch
matrix_fi_reconciliation_case
matrix_fi_reconciliation_participant
matrix_fi_reconciliation_match
matrix_fi_reconciliation_difference
matrix_fi_reconciliation_resolution
```

### 7.1 Rule

`matrix_fi_reconciliation_rule`：

```text
fid
ftenant_id
fcode
fname
fscenario_type
fstatus
fcurrent_version
fpriority
feffective_date
fexpiry_date
fcreate_by
fcreate_time
fmodify_by
fmodify_time
fdelete_flag
fversion
```

Scenario 第一批：

```text
P2P_3WAY_MATCH
AR_COLLECTION
AP_PAYMENT
BANK_COLLECTION
BANK_PAYMENT
AR_SUBLEDGER_GL
AP_SUBLEDGER_GL
```

### 7.2 Rule Version

`matrix_fi_reconciliation_rule_version`：

```text
fid
ftenant_id
frule_id
fversion_no
fstatus
fdefinition_json
fdefinition_hash
fpublished_by
fpublished_time
fcreate_time
```

规则发布后不可直接修改，新的口径创建新版本。

### 7.3 Rule Field

`matrix_fi_reconciliation_rule_field`：

```text
fid
ftenant_id
frule_version_id
ffield_code
ffield_role
fleft_expression
fright_expression
fcompare_operator
ftolerance_type
ftolerance_value
fseverity
frequired
fsort_no
```

`ffield_role`：

```text
MATCH_KEY
QUANTITY
PRICE
AMOUNT
TAX
CURRENCY
DATE
INFO
```

`fcompare_operator`：

```text
EQ
ABS_DIFF_LE
PERCENT_DIFF_LE
IN
DATE_WINDOW
```

P0 不开放任意脚本执行。

## 8. 执行模型

### 8.1 Batch

`matrix_fi_reconciliation_batch`：

```text
fid
ftenant_id
forg_id
fbatch_no
fscenario_type
frule_code
frule_version
frequest_id
fperiod
fstart_date
fend_date
fstatus
ftotal_case_count
fmatched_count
fpartial_count
fdifference_count
funmatched_count
ferror_count
fstarted_time
ffinished_time
fcreate_by
fcreate_time
fmodify_time
fversion
```

状态：

```text
CREATED
RUNNING
COMPLETED
PARTIAL_FAILED
FAILED
CANCELLED
```

同一个 `frequest_id` 幂等。

历史 Batch 不覆盖；重新跑产生新 Batch。

### 8.2 Case

`matrix_fi_reconciliation_case`：

```text
fid
ftenant_id
forg_id
fbatch_id
fcase_no
fmatch_key
fresult
fconfidence
fparticipant_count
fdifference_count
fsource_amount
ftarget_amount
fdifference_amount
fstatus
fcreate_time
fmodify_time
fversion
```

高层结果：

```text
MATCHED
PARTIAL_MATCHED
UNMATCHED
DIFFERENCE
IGNORED
```

不要把 `QUANTITY_DIFFERENCE`、`PRICE_DIFFERENCE` 等塞进主结果状态，这些进入 Difference 明细。

## 9. Difference 模型

`matrix_fi_reconciliation_difference`：

```text
fid
ftenant_id
fcase_id
fdifference_type
ffield_code
fseverity
fexpected_value
factual_value
fdifference_value
ftolerance_value
fblocking_flag
fmessage
fstatus
fcreate_time
```

第一批 DifferenceType：

```text
MISSING_DOCUMENT
QUANTITY_DIFFERENCE
PRICE_DIFFERENCE
AMOUNT_DIFFERENCE
TAX_DIFFERENCE
BATCH_DIFFERENCE
PARTNER_DIFFERENCE
CURRENCY_DIFFERENCE
DATE_DIFFERENCE
DUPLICATED
OVER_SETTLEMENT
OTHER
```

三单匹配原方案明确至少需要：

```text
QUANTITY_DIFFERENCE
PRICE_DIFFERENCE
BATCH_DIFFERENCE
```

## 10. P2P 三单匹配

### 10.1 候选关系

优先使用：

```text
BOTP Document Relation
```

确定：

```text
PO → Receipt
PO/Receipt → Invoice
```

BOTP 是候选关联的首要证据。

如果历史数据没有 BOTP Relation，可按规则允许使用：

```text
PO Number
Supplier
Material
Contract
Delivery Note
Invoice Reference
```

作为补充候选条件，但必须标记 candidate source。

### 10.2 推荐匹配维度

业务需求明确要求的字段：

```text
货品 / 品名
规格
数量
单价
总金额
入库批次
```

财务侧再增加：

```text
businessPartner
currency
taxRate
taxAmount
```

其中 Tax 的具体容差和合规规则以后由税务规则定义。

### 10.3 处理部分收货 / 部分开票

例如：

```text
PO Qty      = 100
Receipt Qty = 60
Invoice Qty = 60
```

若订单尚未关闭，允许结果：

```text
PARTIAL_MATCHED
```

而不是错误。

例如：

```text
PO Qty      = 100
Receipt Qty = 60
Invoice Qty = 80
```

应为：

```text
DIFFERENCE
QUANTITY_DIFFERENCE
```

因为发票量超过当前实际收货量。

### 10.4 容差

框架支持：

```text
quantityTolerance
priceTolerance
amountTolerance
```

但 P0 默认值为 0。

正式容差必须来源于企业采购/财务政策；源方案没有给出统一百分比，因此不得擅自定义 1%、5% 等默认业务口径。

## 11. AR ↔ Collection

规则依据：

```text
收款核销金额不得超过应收余额
```

候选匹配键：

```text
businessPartnerId
currency
salesOrder / contract / invoice reference
```

金额允许部分核销：

```text
AR Open = 10000
Collection = 6000

Reconciliation = PARTIAL_MATCHED
Suggested Writeoff = 6000
```

框架输出 Suggested Allocation，但真正写入：

```text
AR writeoff link
open amount
settlement status
```

必须由 AR Domain Service 执行。

## 12. AP ↔ Payment

规则依据：

```text
付款金额不得超过应付余额
付款银行账户币种必须与付款币种一致
```

示例：

```text
AP Open = 10000
Payment = 12000
```

结果：

```text
DIFFERENCE
OVER_SETTLEMENT
blocking = true
```

例如：

```text
AP Currency = CNY
Payment Currency = USD
```

结果：

```text
DIFFERENCE
CURRENCY_DIFFERENCE
blocking = true
```

现有 FIFO autoWriteoff 后续迁为 `AP_PAYMENT` 的一种 Allocation Strategy：

```text
OLDEST_DUE_FIRST
```

而不是写死在通用框架中。

## 13. Bank Flow ↔ Collection / Payment

财务方案要求银行到账流水匹配业务单据并进行收款认领，付款同理。

Bank Match Candidate 建议字段：

```text
bankAccount
flowDirection
currency
amount
counterpartyAccount
counterpartyName
referenceNo
purpose
businessDocumentNo
transactionDate
```

第一阶段匹配策略：

```text
1. 银行账号 + 业务单号 + 金额完全一致
2. 往来方 + 币种 + 金额 + 日期窗口
3. 往来方 + 金额 + 摘要/用途业务号
4. 无可靠候选 → MANUAL
```

可以计算确定性 score，但不直接由 AI 自动认款。

例如：

```text
referenceNo exact        +40
amount exact             +30
partner exact            +20
date in window           +10
```

Score 只是排序和解释依据，是否自动确认由 Rule Version 决定。

## 14. Subledger ↔ GL

这类场景不是单据一对一，而是聚合余额对账。

### 14.1 AR

财务规则要求：

```text
应收明细账合计
=
总账应收科目余额
```

聚合维度：

```text
tenant
accountingOrg
book
period
account
businessPartner dimension
currency（需要时）
```

### 14.2 AP

同理：

```text
应付明细账合计
=
总账应付科目余额
```

结果保存：

```text
Subledger Balance
GL Balance
Difference
```

不自动生成调整凭证。

差异处理必须进入 Resolution 流程。

## 15. Match Snapshot

`matrix_fi_reconciliation_match` 只保存“本次执行时系统如何判断”的快照，不替代 BOTP Relation 或 AR/AP Writeoff Link。

字段：

```text
fid
ftenant_id
fcase_id
fsource_participant_id
ftarget_participant_id
fmatched_quantity
fmatched_amount
fscore
fstrategy
fauto_flag
fcreate_time
```

例如：

```text
PO.Line1 → Receipt.Line8
matchedQty = 60
strategy = BOTP_RELATION
```

这是对账证据快照，不是新的权威单据关系。

## 16. Resolution

Difference 不应该通过直接修改数据库消失。

新增：

```text
matrix_fi_reconciliation_resolution
```

字段：

```text
fid
ftenant_id
fcase_id
fdifference_id
faction_type
fstatus
fresolution_note
fexternal_action_ref
fresolved_by
fresolved_time
fcreate_time
```

ActionType：

```text
ACCEPT_DIFFERENCE
CORRECT_SOURCE
CORRECT_TARGET
REQUEST_SUPPLIER_CORRECTION
REISSUE_INVOICE
CREATE_ADJUSTMENT
WRITE_OFF
RETRY_MATCH
IGNORE
```

原则：

- Reconciliation Framework 记录 Resolution。
- 真正修改 PO、Receipt、Invoice、AP、AR、Payment 的动作调用所属领域服务。
- 财务调整需要走 Accounting Event / Adjustment Voucher，不允许框架直接写 GL。

## 17. 状态与关闭

Case 状态：

```text
OPEN
RESOLVING
RESOLVED
CLOSED
```

Result 和 Status 分开：

```text
result = DIFFERENCE
status = OPEN
```

人工接受差异后可能成为：

```text
result = DIFFERENCE
status = RESOLVED
```

历史事实仍是“存在差异”，不能把 result 改成 MATCHED 来掩盖。

## 18. 幂等与不可变历史

执行规则：

1. 同一 requestId 只能产生一个 Batch。
2. Batch 固定记录 ruleCode + ruleVersion。
3. 已完成 Batch 的 Participant/Match/Difference 不允许覆盖更新。
4. 规则发生变化后重新执行必须新建 Batch。
5. 手工 Resolution 追加记录，不删除历史 Difference。

这样可以回答：

```text
2026-08-26 为什么判定为差异？
当时用了哪版规则？
哪些字段不一致？
后来谁做了什么处理？
```

## 19. Adapter SPI

统一框架不能直接依赖所有业务表。

定义：

```java
public interface ReconciliationScenarioAdapter {

    String scenarioType();

    List<ReconciliationCandidate> loadCandidates(
        ReconciliationRequest request,
        ReconciliationRuleVersion rule
    );

    List<ReconciliationCaseDraft> buildCases(
        List<ReconciliationCandidate> candidates,
        ReconciliationRuleVersion rule
    );
}
```

第一批实现：

```text
P2pThreeWayReconciliationAdapter
ArCollectionReconciliationAdapter
ApPaymentReconciliationAdapter
BankCollectionReconciliationAdapter
BankPaymentReconciliationAdapter
ArSubledgerGlReconciliationAdapter
ApSubledgerGlReconciliationAdapter
```

各 Adapter 负责取数和领域含义；通用 Engine 负责规则执行、差异生成和持久化。

## 20. Engine

```java
public interface ReconciliationEngine {
    ReconciliationBatchResult execute(ReconciliationRequest request);
}
```

执行：

```text
1. 幂等检查
2. 锁定已发布 Rule Version
3. 创建 Batch
4. Adapter 加载 Candidate
5. 构建 Case
6. 写 Participant Snapshot
7. 执行 Match
8. 执行 Field Compare
9. 生成 Difference
10. 计算 Case Result
11. 汇总 Batch
12. COMPLETED / PARTIAL_FAILED
```

## 21. 查询 API

建议：

```text
POST /reconciliation/batches
GET  /reconciliation/batches
GET  /reconciliation/batches/{batchId}
GET  /reconciliation/batches/{batchId}/cases
GET  /reconciliation/cases/{caseId}
GET  /reconciliation/cases/{caseId}/participants
GET  /reconciliation/cases/{caseId}/differences
POST /reconciliation/cases/{caseId}/resolutions
POST /reconciliation/cases/{caseId}/retry
```

P2P 便捷入口可以放在采购业务 Controller，但内部仍调用统一 Reconciliation Service。

## 22. 前端呈现

三单匹配建议：

```text
采购订单      收货累计      发票累计      结果
100           100           100           MATCHED
100            60            60           PARTIAL_MATCHED
100            60            80           DIFFERENCE
```

展开 Difference：

```text
字段：数量
订单：100
已收：60
已开票：80
差异：20
严重度：BLOCKING
建议：退票重开 / 调整发票
```

单据详情页可以：

```text
BOTP 单据链
      ↓
Reconciliation Case
      ↓
Difference / Resolution
```

实现真正的业务穿透和问题定位。

## 23. P0-06 第一批实现顺序

```text
R1  Reconciliation Rule / Version 数据模型
R2  Batch / Case / Participant / Difference 数据模型
R3  Reconciliation Engine + Adapter SPI
R4  P2P 3-Way Match Adapter
R5  AR/AP 现有 AutoWriteoff 接入统一 Case/Allocation
R6  Bank Flow Match Adapter
R7  AR/AP Subledger ↔ GL
R8  Resolution / Manual Handling API
```

## 24. 第一条验收用例

PO：

```text
PO001.Line1
Qty = 100
Price = 100
Amount = 10000
```

Receipt：

```text
GR001.Line1 Qty = 60
GR002.Line1 Qty = 40
```

Invoice：

```text
INV001.Line1 Qty = 100
Price = 100
Amount = 10000
```

BOTP 已记录 PO → GR1 / GR2。

执行：

```text
P2P_3WAY_MATCH
```

期望：

```text
Case = C001
Participant Count = 4
Result = MATCHED
Difference Count = 0
```

再将发票改为：

```text
Qty = 110
Amount = 11000
```

新 Batch 必须得到：

```text
Result = DIFFERENCE
QUANTITY_DIFFERENCE
AMOUNT_DIFFERENCE
blocking = true
```

旧 Batch 结果保持不变。

## 25. P0-06 验收标准

至少满足：

- 三单完全一致 → MATCHED。
- 合法部分收货/开票 → PARTIAL_MATCHED。
- 数量/价格/金额/批次差异 → 字段级 Difference。
- AP 付款超过余额 → BLOCKING。
- AR 收款核销超过余额 → BLOCKING。
- 币种不一致 → CURRENCY_DIFFERENCE。
- BOTP Relation 可作为候选证据，但不替代 Reconciliation Result。
- Reconciliation 不直接改变 AR/AP 余额。
- 重复 requestId 不重复建 Batch。
- 新规则版本重跑形成新 Batch，历史结果不可覆盖。
- AR/AP 明细账与 GL 可按期间核对并产出差异。
- 所有人工处理动作有 Resolution 审计轨迹。

P0-06 完成后，Matrix 将具备从“单据有关联”进一步判断到“数据是否一致、差异在哪里、如何闭环处理”的平台能力。