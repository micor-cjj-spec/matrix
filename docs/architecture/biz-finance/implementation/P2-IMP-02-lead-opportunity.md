# P2-IMP-02 Lead / Opportunity 实现记录

> 状态：Implemented v1  
> 日期：2026-08-27  
> 目标分支：dev

## 1. 目标

建立 O2C 第一段 CRM 经营事实：

```text
Lead
→ Qualification
→ Opportunity
→ Won / Lost
```

本阶段不进入报价、合同、AR 或凭证。

## 2. Schema

新增：

```text
deliverables/erp/011-crm-lead-opportunity/schema.sql
```

表：

```text
matrix_erp_crm_lead
matrix_erp_crm_opportunity
```

## 3. Lead

Lead 可以在尚未形成 Customer 主数据时存在。

状态：

```text
NEW
→ QUALIFYING
→ QUALIFIED
→ CONVERTED

异常：
DISQUALIFIED
```

支持：

- create
- update
- page
- detail
- startQualification
- qualify
- disqualify
- delete NEW lead

QUALIFIED 时同事务写：

```text
CRM_LEAD_QUALIFIED
routingKey:
biz.crm.lead.qualified
```

## 4. Opportunity Customer Gate

Opportunity 必须引用稳定 BusinessPartner。

ERP 通过 Feign 调用 base-service：

```text
GET /business-partners/{fid}?tenantId=...
```

校验：

```text
tenant match
fstatus = ACTIVE
fapprovalStatus = AUDITED
roles contains CUSTOMER
```

商机保存：

```text
businessPartnerId   权威引用
businessPartnerCode 历史快照
businessPartnerName 历史快照
```

## 5. Lead → Opportunity

当 Opportunity create 携带 leadId：

```text
validate Customer
→ SELECT Lead FOR UPDATE
→ require Lead = QUALIFIED
→ create Opportunity
→ Lead = CONVERTED
→ write convertedBusinessPartnerId
→ write convertedOpportunityId
```

同事务 Outbox：

```text
CRM_LEAD_CONVERTED
CRM_OPPORTUNITY_CREATED
```

## 6. Opportunity 状态

生命周期：

```text
OPEN
→ WON
→ LOST
```

OPEN 阶段：

```text
DISCOVERY
QUALIFICATION
PROPOSAL
NEGOTIATION
```

终态：

```text
WON
LOST
```

概率：

```text
OPEN: [0,100)
WON: 100
LOST: 0
```

赢单：

```text
CRM_OPPORTUNITY_WON
→ biz.crm.opportunity.won
```

丢单：

```text
CRM_OPPORTUNITY_LOST
→ biz.crm.opportunity.lost
```

这些仍然是经营事件，不产生财务结果。

## 7. Business Event 扩展

`BusinessEventOutboxService` 保留既有 Procurement append 签名。

新增 domain-aware overload：

```text
domainCode
eventType
...
```

CRM routing：

```text
CRM_LEAD_QUALIFIED     → biz.crm.lead.qualified
CRM_LEAD_CONVERTED     → biz.crm.lead.converted
CRM_OPPORTUNITY_CREATED→ biz.crm.opportunity.created
CRM_OPPORTUNITY_WON    → biz.crm.opportunity.won
CRM_OPPORTUNITY_LOST   → biz.crm.opportunity.lost
```

CRM 当前还没有强制消费者，因此没有加入 required-routing-keys。

## 8. API

Lead：

```text
POST   /crm/leads
PUT    /crm/leads/{fid}
GET    /crm/leads
GET    /crm/leads/{fid}
POST   /crm/leads/{fid}/start-qualification
POST   /crm/leads/{fid}/qualify
POST   /crm/leads/{fid}/disqualify
DELETE /crm/leads/{fid}
```

Opportunity：

```text
POST /crm/opportunities
PUT  /crm/opportunities/{fid}
GET  /crm/opportunities
GET  /crm/opportunities/{fid}
POST /crm/opportunities/{fid}/stage
POST /crm/opportunities/{fid}/win
POST /crm/opportunities/{fid}/lose
```

## 9. Tests

新增：

```text
CrmLeadServiceTest
CrmOpportunityServiceTest
```

覆盖：

1. Lead 创建不依赖 Customer。
2. Lead qualify 写 CRM Outbox。
3. 已转换 Lead 不允许 disqualify。
4. QUALIFIED Lead 转换 Opportunity。
5. Customer Gate 失败拒绝创建商机。
6. Opportunity win 固化 WON / probability=100 并写经营事件。

## 10. PR / CI

实现 PR：

```text
PR #92
feat(crm): implement P2 lead and opportunity
```

head：

```text
5718f6ab1f7cddb7404d7b80dbdd607fb588177b
```

merge commit：

```text
a2e9b40ed94913d183b723ccf60b11596cf81543
```

CI：

```text
Repository Hygiene CI 33061516194 success
Biz Finance P0 CI     33061516256 success
```

Biz Finance P0 CI 同时回归：

```text
erp-service
fi-service
botp-service
```

## 11. 当前边界

本阶段未实现：

- CRM frontend
- Quote / Tender
- SalesContract
- SalesOrder
- AR
- Credit Gate
- CRM BOTP
- 自动经营评分模型

## 12. 下一阶段

```text
P2-IMP-03 Quote / Tender + SalesContract
```
