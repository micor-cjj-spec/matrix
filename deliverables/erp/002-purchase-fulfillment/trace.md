# P0-IMP-02 Trace 示例

```text
PurchaseOrder PO001 / Line 11
  ↓ BOTP RelationEntry qty=60
PurchaseReceipt PRC001 / Line 21
  ↓ BOTP RelationEntry inspection qty=60
PurchaseAcceptance PAC001 / Line 31
  ↓ BOTP RelationEntry inbound qty=50
PurchaseInbound PIN001 / Line 41
  ↓ PURCHASE_INBOUND_CONFIRMED
matrix_erp_business_event_outbox
```

P0-IMP-03 将从最后一条 Business Event 接入：

```text
FI Inbox
→ AP Estimate
→ Accounting Event
→ Voucher
```
