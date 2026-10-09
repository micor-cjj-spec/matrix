# ADR-001：使用 BusinessPartner 统一客户与供应商主体

- 状态：Accepted
- 日期：2026-08-26

## Context

客户和供应商可能指向同一法人主体。若分别维护 Customer/Supplier 主表，会造成重复法人、税号、银行账户、地址和结算信息，并给 AR/AP、合同、信用和对账带来主数据一致性问题。

## Decision

采用统一 `BusinessPartner` 主体 + 多角色模型。

```text
BusinessPartner
  ├─ CUSTOMER
  └─ SUPPLIER
```

主体保存法人/个人公共身份信息；客户、供应商作为 Role；组织可用范围、银行、税务和结算档案作为关联对象。

现有 `/customer/**`、`/supplier/**` 通过 Facade 保持兼容。

## Consequences

优点：

- 同一法人只维护一份主体。
- 支持同时为客户和供应商。
- AR/AP 可统一引用 `businessPartnerId`。
- 后续合同、信用、对账、AI 穿透拥有稳定主体 ID。

代价：

- 需要兼容旧 Customer/Supplier API。
- 历史字符串往来单位需逐步迁移为 ID + Snapshot。
- CRM/SRM 必须明确哪些属性属于 Role 而不是 Partner 主体。
