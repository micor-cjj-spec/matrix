# P0-IMP-02 验收检查清单

- [ ] PO 100，下推 Receipt 60，预占=60。
- [ ] Receipt 60 确认后 received=60、reserved=0。
- [ ] 再收货 40 后 PO receiptStatus=COMPLETE。
- [ ] 剩余 40 时两个并发收货 30，最多一个成功预占。
- [ ] 验收数量=合格+让步+不合格，否则提交失败。
- [ ] 全不合格验收不允许入库。
- [ ] 可入库 50 时 30+20 成功，累计 51 失败。
- [ ] 入库确认后 accountingStatus=PENDING。
- [ ] 入库确认和 PURCHASE_INBOUND_CONFIRMED Outbox 同事务。
- [ ] BOTP RelationEntry 保存 sourceEntryId/targetEntryId/quantity。
- [ ] BOTP FAILED execution 使用原 executionId 重试且目标不重复创建。
- [ ] 相同 documentId 不同 documentType 的 TargetStatusEvent 不串行失效。
