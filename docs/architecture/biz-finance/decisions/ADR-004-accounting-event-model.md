# ADR-004：Business Event 与 Accounting Event 分层

- 状态：Accepted
- 日期：2026-08-26

## Context

如果业务模块直接生成凭证，或者把 Business Event 命名成“创建应付凭证”，业务域会与财务科目、辅助核算和凭证实现强耦合，后续规则调整必须修改业务代码，也无法让同一个业务事实被项目成本、税务、AI 等多个领域复用。

## Decision

采用三层语义：

```text
Business Event
= 发生了什么

Accounting Event
= 这件事在财务上意味着什么

Accounting Rule
= 应该如何核算
```

例如：

```text
PURCHASE_RECEIPT_CONFIRMED
        ↓
PURCHASE_RECEIPT_ESTIMATE_RECOGNITION
        ↓
PURCHASE_RECEIPT_ESTIMATE Rule V3
        ↓
Accounting Result
        ↓
Voucher
```

一个 Business Event 可以产生多个 Accounting Event，例如供应商发票确认后同时产生暂估冲回和正式应付确认。

## Rule Versioning

Accounting Rule 使用主表 + 不可变版本：

```text
Rule
→ RuleVersion
→ RuleEntry
→ RuleDimension
```

历史 Accounting Event 固定记录实际使用的 ruleCode/ruleVersion。

冲销基于原 Accounting Result 快照反向生成，不得用当前最新规则重算历史业务。

## Consequences

优点：

- 业务模块不感知科目与凭证。
- 会计规则可以独立版本化和治理。
- 支持源单 → Business Event → Accounting Event → Rule → Voucher → GL 全链路追溯。
- 允许同一 Business Event 被多个领域独立消费。

代价：

- 财务域增加 Accounting Engine、Resolver、Trace 等基础设施。
- 需要维护事件契约与规则版本兼容。
- 规则未命中或冲突时必须进入明确异常状态，不能静默兜底。
