# ADR-007: P2P 以采购入库而不是采购收货作为暂估核算触发点

- 状态：Accepted
- 日期：2026-08-26

## Context

前序 P0 示例曾使用 `PURCHASE_RECEIPT_CONFIRMED` 直接触发暂估应付和采购入库核算。

在 P0-07 将采购到付款真实流程与《业财一体项目详细设计方案1.0》逐步对齐后，业务源方案明确区分：

```text
采购收货
→ 检验/验收
→ 采购入库
```

并明确：

```text
根据采购入库单确认暂估应付
```

因此“收货”只是物理到货事实，尚不能证明货物已验收并形成正式库存；如果在收货阶段确认暂估，会把不合格、待检或最终退货的物资提前带入财务核算。

## Decision

Matrix P2P 统一定义三个独立业务事实：

```text
PURCHASE_RECEIPT_CONFIRMED
= 实物已经收货

PURCHASE_ACCEPTANCE_CONFIRMED
= 验收结果已经确认

PURCHASE_INBOUND_CONFIRMED
= 合格/让步接收数量已经正式入库
```

其中只有：

```text
PURCHASE_INBOUND_CONFIRMED
```

默认触发：

```text
AP Estimate
+
PURCHASE_INBOUND_ESTIMATE_RECOGNITION
```

并进入 Accounting Rule → Voucher → GL。

收货和验收事件仍可被库存、项目、供应商绩效、通知、AI 等消费者使用，但不直接确认暂估应付。

## P2P Authority Boundary

同时确定 P2P 各阶段权威对象：

```text
PurchaseOrder       = 采购约定
PurchaseReceipt     = 实物到货
PurchaseAcceptance  = 验收结论
PurchaseInbound     = 正式入库
SupplierInvoice     = 供应商发票业务事实
AP Payable          = 应付余额
PaymentApplication  = 付款请求/金额预占
PaymentOrder        = 支付指令
BankTransaction     = 银行事实
AP Settlement       = 核销事实
Voucher / GL        = 会计结果
```

并继续遵守：

```text
BOTP Relation
≠ Reconciliation
≠ Settlement
≠ Accounting
```

## Consequences

### Positive

- 与源业务方案“收货→验收→入库→暂估”一致。
- 不合格/退货物资不会因到货就错误确认财务暂估。
- 库存、AP、Accounting Event 之间的触发边界更加清晰。
- 后续退货、让步接收、部分验收可以自然建模。

### Trade-offs

- 比“Receipt 一张单据包办到底”多两个业务对象。
- P0 第一条链实现工作量增加。
- 前序 P0 示例需要按本 ADR 的语义解释；后续实现以本 ADR 为准。

## Compatibility

当前 Matrix 尚没有正式采购领域实现，因此该修正不会造成生产数据迁移。

现有 `BizfiFiArapDoc`、`AP_PAYMENT_APPLY`、`AP_PAYMENT_PROCESS` 等历史财务对象继续保留兼容，但新 P2P 业务模型以 P0-07 的对象边界为目标。
