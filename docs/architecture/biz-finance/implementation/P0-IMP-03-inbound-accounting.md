# P0-IMP-03 Inbound → AP Estimate → Voucher 实现记录

> 状态：Implemented v1  
> 日期：2026-08-26

## 1. 实现目标

本阶段把 P0-IMP-02 已产生的采购入库业务事实真正接入财务核算：

```text
PurchaseInbound CONFIRMED
→ ERP Transactional Outbox
→ RabbitMQ Business Event
→ FI Inbox
→ AP Estimate
→ Accounting Event
→ Accounting Rule
→ Voucher Draft
→ Accounting Trace
```

业务触发事实固定为：

```text
PURCHASE_INBOUND_CONFIRMED
```

采购收货或验收本身不形成暂估应付。

## 2. ERP Outbox Publisher

`erp-service` 在已有 `matrix_erp_business_event_outbox` 基础上增加真正的 Dispatcher：

- `PENDING/FAILED → PUBLISHING → PUBLISHED`
- 多实例通过数据库 claim 抢占发送权。
- publisher confirm 确认 Exchange 已接收消息。
- publisher return 检测关键事件是否不可路由。
- 失败指数退避，达到 `fmax_retry` 后进入 `DEAD`。
- 超时 `PUBLISHING` claim 自动恢复为 `FAILED`。

Exchange：

```text
matrix.business.events
```

入库 routing key：

```text
biz.procurement.purchase_inbound.confirmed
```

P0 默认只把上述入库事件配置为 `required-routing-key`。收货/验收事件即使当前没有订阅者，也不会因为无人消费而永久失败。

## 3. Business Event Envelope

ERP Outbox Payload 不直接裸发，而是组装统一 Envelope：

```text
eventId
eventType
eventVersion
tenantId
orgId
producerService
domainCode
aggregateType
aggregateId
aggregateVersion
sourceSystemCode
sourceDocumentType
sourceDocumentId
sourceDocumentNo
businessDate
correlationId
causationId
traceId
operatorId
payload
```

财务消费者只处理：

```text
eventType = PURCHASE_INBOUND_CONFIRMED
eventVersion = 1
sourceDocumentType = ERP_PURCHASE_INBOUND
```

## 4. FI Queue / DLQ

`fi-service` 增加独立的持久化消费队列：

```text
matrix.fi.purchase-inbound-accounting
```

绑定：

```text
matrix.business.events
+ biz.procurement.purchase_inbound.confirmed
```

失败消息不做 Broker 无限 requeue，而进入：

```text
matrix.fi.business-event.dead
matrix.fi.purchase-inbound-accounting.dead
```

失败原因同时落 FI Inbox，供后续人工修复/重放。

## 5. FI Inbox 幂等

新增：

```text
matrix_fi_inbox_event
```

唯一键：

```text
fconsumer_code + fevent_id
```

当前 consumer code：

```text
FI_PURCHASE_INBOUND_ACCOUNTING_V1
```

消费成功状态：

```text
PROCESSING → PROCESSED
```

处理失败时主业务事务回滚，然后使用独立 `REQUIRES_NEW` 事务记录：

```text
FAILED + error_message
```

因此 Broker 重复投递不能重复生成应付或凭证。

## 6. AP Estimate

本阶段不继续扩充历史 `bizfi_fi_arap_doc`，新增规范化对象：

```text
matrix_fi_ap_payable
matrix_fi_ap_payable_entry
```

暂估应付：

```text
ftype = ESTIMATE
fstatus = OPEN
fapproval_status = AUDITED
faccounting_status = PENDING / VOUCHER_GENERATED
```

应付保存业务伙伴 ID，同时保存 Code/Name 历史快照；分录保存来源入库行、采购订单行、物料、仓库、项目、成本中心、数量、单价和金额。

业务事件唯一约束：

```text
ftenant_id + fbusiness_event_id
```

## 7. Accounting Event + Rule

新增：

```text
matrix_fi_accounting_event
matrix_fi_accounting_event_entry
matrix_fi_accounting_event_dimension
matrix_fi_accounting_rule
matrix_fi_accounting_rule_version
matrix_fi_accounting_rule_entry
matrix_fi_accounting_rule_dimension
matrix_fi_account_mapping
```

转换关系：

```text
PURCHASE_INBOUND_CONFIRMED
→ PURCHASE_INBOUND_ESTIMATE_RECOGNITION
```

规则命中按：

```text
priority
+ specificity
```

决定最高等级；最高等级出现多条规则时直接报：

```text
ACCOUNTING_RULE_CONFLICT
```

不能随机取第一条。

## 8. 受控规则能力

P0 不引入 Groovy / JavaScript / SpEL 任意脚本。

金额表达式仅允许受控能力：

```text
FIELD(entry.amount)
FIELD(payload.totalAmount)
SUM(entries.amount)
```

Rule Entry 支持：

```text
HEADER / ENTRY
DEBIT / CREDIT
FIXED / MAPPING
```

生成 Accounting Result 后强制：

```text
Debit Total = Credit Total
```

否则 `ACCOUNTING_UNBALANCED`。

## 9. 辅助核算维度

新增：

```text
matrix_fi_accounting_event_dimension
matrix_fi_voucher_line_dimension
```

第一条 P2P 链支持：

```text
PROJECT
COST_CENTER
BUSINESS_PARTNER
```

维度采用 ID 作为关联权威，同时保存 Code/Name 快照，避免历史凭证展示依赖当前主数据名称。

## 10. Voucher Draft

继续复用现有：

```text
BizfiFiVoucher
BizfiFiVoucherLine
BizfiFiVoucherService
```

不新造第二套凭证内核。

自动核算的凭证幂等键：

```text
source_request_id = ACCOUNTING:{accountingEventId}:VOUCHER:0
```

本阶段只生成：

```text
DRAFT
```

不会自动 Submit / Audit / Post，不绕过现有财务凭证生命周期。

## 11. Accounting Trace

新增：

```text
matrix_fi_accounting_trace
```

形成穿透链：

```text
Business Event
→ AP Payable
→ Accounting Event
→ Rule + Version
→ Voucher
```

Accounting Event 同时保存实际解析后的科目和辅助核算结果，所以后续规则变化不改变历史核算事实。

## 12. 兼容科目 Seed

迁移文件提供 `COMPATIBILITY` Seed：

```text
PURCHASE_INBOUND_DEBIT → 1405
ESTIMATED_AP           → 2202
```

这两个编码只来自 Matrix 现有 AP_ESTIMATE 实现，是为了把旧 Java 固定映射迁出代码，并非正式财务核算规则标准。

完整核算规则标准表到位后，应发布新的正式规则/映射版本替换 Compatibility 配置，业务消费代码无需修改。

发布后的 RuleVersion 使用 `INSERT IGNORE` 方式初始化，迁移脚本重复执行不会修改已发布历史版本。

## 13. 事务边界

FI 消费主链在同一个 `fi-service` 本地事务中完成：

```text
Inbox
+ AP Estimate
+ Accounting Event
+ Accounting Result
+ Voucher Draft
+ Voucher Dimensions
+ Trace
```

任何一步失败全部回滚。

这里明确不使用：

```text
FI consumer → 直接更新 ERP 数据库
```

因此 `PurchaseInbound.faccounting_status` 在本阶段仍保持 `PENDING`。后续如果需要回写 `VOUCHER_GENERATED/POSTED`，应通过 FI Outbox/Business Event 回写 ERP，而不是跨服务直连数据库。

## 14. 数据库迁移

```text
deliverables/fi/003-inbound-accounting/schema.sql
deliverables/fi/003-inbound-accounting/compatibility-seed.sql
```

新增对象全部遵守 `docs/specs/database-naming-convention.md`：

```text
matrix_fi_*
f*
fid
ftenant_id
forg_id
```

历史 `bizfi_fi_voucher*` 只做兼容复用，不作为新表命名模板。

## 15. 测试与验收

自动测试至少覆盖：

1. Business Event Envelope 正确解析。
2. `FIELD(entry.amount)` 正确取 ENTRY 金额。
3. `FIELD(payload.totalAmount)` 正确取 HEADER 金额。
4. `SUM(entries.amount)` 正确汇总。
5. 非受控表达式被拒绝。
6. 同一最高 priority/specificity 的多规则冲突被拒绝。
7. 两个 ENTRY 借方 60+40 与 HEADER 贷方 100 生成平衡结果。

端到端验收应验证：

```text
Inbound 100 CONFIRMED
→ ERP Outbox PUBLISHED
→ FI Inbox PROCESSED
→ AP Estimate 100 OPEN
→ Accounting Event VOUCHER_GENERATED
→ Voucher DRAFT 100
→ Trace 完整
```

重复投递同一 `eventId`：

```text
AP 数量不增加
Accounting Event 不增加
Voucher 不增加
```

MQ 不可用时：

```text
ERP 业务事务不回滚
Outbox 保持 FAILED 并重试
```

FI 处理失败时：

```text
财务业务数据回滚
Inbox FAILED
消息进入 DLQ
```

## 16. 当前限制

- 尚未实现正式采购发票与三单匹配，这是 P0-IMP-04。
- 尚未实现暂估冲回 + 正式应付，这是 P0-IMP-05。
- 当前 Compatibility 科目不是正式核算标准。
- 当前没有跨服务回写 ERP 入库核算状态。
- `matrix-prp` 继续保持只读，没有任何修改。
