# ADR-006: Reconciliation 是一致性评估，不是单据关系或核销执行

- 状态：Accepted
- 日期：2026-08-26

## Context

Matrix 已有三类容易被混用的能力：

1. BOTP Document Relation：记录源单、目标单、分录和履约关系。
2. AR/AP Write-off：匹配收付款与应收应付并形成核销链接。
3. OpenAPI Reconcile：按 `source_request_id` 查询异步凭证写入结果。

业财一体和财务核算需求又提出三单匹配、收付款核销、银行流水认领、应收应付明细账与总账一致性等更广泛的对账场景。

如果继续把这些能力分别写进采购、AR、AP、资金和 GL，会出现规则重复、结果不可统一追溯、差异处理无法审计的问题。

## Decision

建立统一 `Reconciliation Framework`，第一阶段位于 `fi-service`。

明确三个边界：

```text
BOTP Relation
= 单据为什么有关联、履约了多少

Reconciliation
= 已有关联/候选数据是否一致、差异在哪里

Write-off / Settlement
= 确认后如何改变业务余额和结算状态
```

Reconciliation Framework：

- 维护规则及不可变版本。
- 执行 Batch / Case。
- 保存 Participant Snapshot。
- 记录 Match Snapshot。
- 生成字段级 Difference。
- 记录 Resolution。
- 不直接修改业务源单余额。
- 不直接生成总账调整分录。

真正的业务动作必须回到所属领域：

```text
采购差异 → Procurement / Invoice
AR 核销 → AR Domain
AP 核销 → AP Domain
银行认款 → Fund Domain
财务调整 → Accounting Event / Voucher
```

## Consequences

### Positive

- 三单匹配、AR/AP、银行流水、子账总账共用统一执行与审计模型。
- BOTP 不被迫承担财务比较规则。
- 核销不被等同于“对账结果”。
- 可保留历史规则版本、差异和人工处理轨迹。
- 后续 AI 可基于结构化 Difference / Resolution 解释差异原因。

### Trade-offs

- 同一个业务场景可能同时存在 BOTP Relation、Reconciliation Case、Write-off Link 三类记录。
- 需要通过 Trace / DocumentRef 明确它们之间的关联。
- 第一阶段 `fi-service` 会增加一个新的领域包，未来若容量和组织边界需要，可再拆为独立服务。

## Implementation Notes

P0 第一批 Scenario：

```text
P2P_3WAY_MATCH
AR_COLLECTION
AP_PAYMENT
BANK_COLLECTION
BANK_PAYMENT
AR_SUBLEDGER_GL
AP_SUBLEDGER_GL
```

高层 Case Result：

```text
MATCHED
PARTIAL_MATCHED
UNMATCHED
DIFFERENCE
IGNORED
```

数量、价格、金额、批次、币种等具体原因进入 Difference，不扩散成大量主状态枚举。
