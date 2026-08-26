# P0-IMP-02 BOTP 规则配置说明

P0-IMP-02 不把采购转换规则硬编码进 Java。规则继续通过现有 BOTP Rule API 维护和发布。

首批规则：

```text
PURCHASE_ORDER_TO_RECEIPT
MATRIX / ERP_PURCHASE_ORDER
→ MATRIX / ERP_PURCHASE_RECEIPT

PURCHASE_RECEIPT_TO_ACCEPTANCE
MATRIX / ERP_PURCHASE_RECEIPT
→ MATRIX / ERP_PURCHASE_ACCEPTANCE

PURCHASE_ACCEPTANCE_TO_INBOUND
MATRIX / ERP_PURCHASE_ACCEPTANCE
→ MATRIX / ERP_PURCHASE_INBOUND
```

核心分录映射：

| 规则 | 源字段 | 目标字段 |
|---|---|---|
| PO→Receipt | `entryId` | `purchaseOrderEntryId` |
| PO→Receipt | `availableQuantity` | `quantity` |
| Receipt→Acceptance | `entryId` | `purchaseReceiptEntryId` |
| Receipt→Acceptance | `availableQuantity` | `inspectionQuantity` |
| Acceptance→Inbound | `entryId` | `purchaseAcceptanceEntryId` |
| Acceptance→Inbound | `availableQuantity` | `quantity` |

单头至少映射：

```text
tenantId
orgId
date
businessPartnerId
businessPartnerCode
businessPartnerName
currencyCode
```

Receipt→Acceptance 还需映射：

```text
purchaseReceiptId
```

Acceptance→Inbound 还需映射：

```text
purchaseAcceptanceId
warehouseId（如适用）
```

部分下推由 ExecutionRequest 参数控制：

```json
{
  "entryQuantities": {
    "<sourceEntryId>": 60
  }
}
```

该参数只是转换输入，不能绕过 ERP 领域服务对剩余量、预占量和源单状态的校验。
