# ADR-002：BOTP 作为统一单据转换与关系基础设施

- 状态：Accepted
- 日期：2026-08-26

## Context

Matrix 已存在 `botp-service`，具备单据类型、转换规则、规则版本、字段映射、执行幂等、单据关系、分录关系表、反写任务和 Outbox 设计。若 ERP 再建设一套通用 `biz_document` / `biz_document_relation`，会形成重复基础设施和双重权威。

## Decision

不新增第二套通用单据关系中心。继续使用并增强 BOTP：

- 业务表保持领域专用 typed table。
- BOTP 统一保存跨域单据关系。
- 正式接入分录级 RelationEntry。
- 增加 RelationType。
- 所有引用使用完整 DocumentKey。
- 增加 upstream/downstream/graph 穿透能力。

## Boundaries

BOTP 负责：

```text
Document Transformation
Document Relation
Entry Relation
Allocation / Writeback
```

BOTP 不负责：

```text
业务合法性规则
三单匹配
Accounting Rule
Voucher
```

## Consequences

优点：

- 复用现有实现和 schema。
- P2P/O2C/项目链可以统一穿透。
- 部分下推、多对一、一对多可由 RelationEntry 表达。

代价：

- 现有 BOTP Java 代码需要补齐 RelationEntry。
- 现有只按 documentId 的部分 Repository 查询必须修正。
- Adapter 需要引入 Reservation，处理并发超下推。
