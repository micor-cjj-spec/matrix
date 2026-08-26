# ADR-005：Accounting Result 统一生成 Voucher，GL 采用不可变过账结果

- 状态：Accepted
- 日期：2026-08-26

## 背景

Matrix 当前 AR/AP 存在按单据类型硬编码借贷科目并直接插入凭证的实现，现有 Voucher 生命周期也缺少财务核算方案要求的“复核”步骤，凭证分录和 GL Entry 尚未形成动态辅助核算维度模型。

随着 P0-04 引入 Accounting Event / Accounting Rule，如果继续允许 AR/AP、Expense、Fund 等领域各自在 Service 内直接决定会计科目，会重新形成多套核算逻辑，无法保证规则版本、凭证和源业务之间的统一审计链。

## 决策

1. 所有内部业财自动核算统一经过：

```text
Business Event
→ Accounting Event
→ Accounting Rule
→ Accounting Result
→ AccountingVoucherService
→ Voucher
→ GL
```

2. Voucher Service 不重新计算会计规则，只负责持久化 Accounting Result 和执行凭证生命周期。
3. Accounting Event 来源凭证通过 `source_request_id` 做最终凭证幂等。
4. Voucher 生命周期扩展为：

```text
DRAFT → SUBMITTED → REVIEWED → AUDITED → POSTED
```

5. 复核人与制单人必须不同。
6. 新增 `matrix_fi_voucher_line_dimension` 保存凭证辅助核算维度快照。
7. 新增 `matrix_fi_gl_entry_dimension` 保存已过账维度快照。
8. 已过账 GL Entry 不允许通过删除后重建实现幂等；采用事务 + 唯一约束 + 状态幂等。
9. 冲销基于原 Accounting Result / Voucher 实际结果反向生成，不重新执行当前最新版会计规则。
10. OpenAPI 直接写凭证草稿保持独立，标记 `sourceType=OPENAPI`，不强制转换为 Accounting Event。

## 后果

优点：

- AR/AP、Expense、Asset、Fund 等模块使用同一自动核算底座。
- 科目和辅助核算规则可以版本化、审计和回放。
- 支持业务源单 → 会计事件 → 规则版本 → 凭证 → 总账完整穿透。
- 避免同一业务在不同 Service 中维护不同硬编码科目。
- 总账维度具备稳定历史快照。

代价：

- Voucher 状态机需要兼容增加 REVIEWED。
- 现有 AR/AP 直接生成凭证逻辑需要渐进迁移。
- 凭证和 GL 需要增加维度表及相关查询能力。

## 不采用的方案

### 方案 A：各业务模块直接生成凭证

否决。会形成重复会计规则和强业务财务耦合。

### 方案 B：Voucher Service 根据 sourceDocumentType 再匹配会计规则

否决。Accounting Engine 和 Voucher 层会重复计算，且无法保证规则结果快照一致。

### 方案 C：POST 时删除旧 GL Entry 后重建

否决。正式账簿结果应不可变；幂等应由状态、事务和唯一约束保证。
