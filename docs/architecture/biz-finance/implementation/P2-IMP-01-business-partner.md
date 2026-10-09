# P2-IMP-01 BusinessPartner + Customer/Supplier Persistence 实现记录

> 状态：Implemented v1  
> 日期：2026-08-27  
> 目标分支：dev

## 1. 目标

P2 O2C 在进入 Lead / Opportunity 之前，先兑现 P0 已设计但尚未落地的统一客商中心。

旧实现：

```text
/customer/**
/supplier/**
→ BaseDataWorkflowController
→ ConcurrentHashMap
```

存在：

- 服务重启数据丢失。
- Customer / Supplier 各自作为孤立基础资料。
- 没有稳定 BusinessPartner ID。
- CRM / Sales / AR 无法建立统一法人引用。

P2-IMP-01 改为：

```text
Customer / Supplier Compatibility API
→ BusinessPartnerService
→ matrix_base_business_partner
→ matrix_base_business_partner_role
```

## 2. Schema

新增：

```text
deliverables/base/001-business-partner/schema.sql
```

数据库：

```text
matrix_base
```

首批表：

```text
matrix_base_business_partner
matrix_base_business_partner_role
matrix_base_business_partner_org
matrix_base_business_partner_contact
matrix_base_business_partner_address
matrix_base_business_partner_bank_account
matrix_base_business_partner_tax_profile
matrix_base_business_partner_settlement
```

全部遵守：

```text
matrix_{module}
matrix_{module}_{business_table}
f*
fid
ftenant_id
fdelete_flag
fversion
```

## 3. 主体与角色

统一主体：

```text
BusinessPartner
```

业务角色：

```text
CUSTOMER
SUPPLIER
```

同一编码或统一社会信用代码命中同一法人时，Customer / Supplier 复用同一个 Partner ID。

如果：

```text
same fcode
+
different unified social credit code
```

则拒绝自动合并。

不允许通过名称相似度自动合并法人。

## 4. 状态分离

数据库内部：

```text
BusinessPartner.fstatus
= 生命周期

DRAFT
ACTIVE
DISABLED
CLOSED
```

```text
BusinessPartner.fapproval_status
= 审批状态

DRAFT
SUBMITTED
AUDITED
REJECTED
```

旧 Customer / Supplier 页面仍接收：

```text
fstatus = approvalStatus
```

因此 matrix-web 无需修改现有状态判断。

## 5. Compatibility API

继续保留：

```text
GET    /customer/list
GET    /customer/{fid}
POST   /customer
PUT    /customer
DELETE /customer/{fid}
POST   /customer/{fid}/submit
POST   /customer/{fid}/audit
POST   /customer/{fid}/reject
```

Supplier 同理。

兼容接口没有显式 tenant 时，v1 使用：

```text
default
```

作为历史接口 fallback。

新领域调用不应依赖这个 fallback。

## 6. BusinessPartner Identity API

新增：

```text
GET /business-partners/{fid}?tenantId=...
GET /business-partners/resolve?tenantId=...&code=...
```

返回：

- stable BusinessPartner ID
- tenant
- code
- name
- partnerType
- unified social credit code
- lifecycle status
- approval status
- roles

后续 CRM / Sales / AR 使用该 ID 作为客户权威引用。

## 7. 内存 Store 清理

`BaseDataWorkflowController` 已移除：

```text
/customer/**
/supplier/**
```

避免：

- 双写
- 数据漂移
- Spring Ambiguous Mapping

其它 material / currency / region 等旧基础资料暂不在本阶段重构。

## 8. Identity Merge Guard

自动复用顺序：

```text
fcode
→ unifiedSocialCreditCode
```

如果两者分别命中两个不同 Partner：

```text
reject
```

如果同一个 fcode 已存在且新请求提供不同统一社会信用代码：

```text
reject
```

已审核 Partner 不能通过旧兼容接口补录统一社会信用代码。

## 9. Tests

新增：

```text
BusinessPartnerServiceTest
```

覆盖：

1. Customer 创建 Partner + CUSTOMER Role。
2. 相同编码 Supplier 复用原 Partner。
3. 审核后内部生命周期 ACTIVE，旧接口仍返回 AUDITED。
4. 同编码但统一社会信用代码冲突时拒绝自动合并。

## 10. CI

新增：

```text
Base Business Partner CI
```

执行：

```text
mvn -pl base-service -am -DskipTests compile
mvn -pl base-service -am -Dtest=BusinessPartnerServiceTest test
```

实现 PR：

```text
PR #91
feat(base): implement persistent business partner center
```

PR head：

```text
38dee129f4a467ead5351b63c08b4122f115444e
```

merge commit：

```text
3fc2987a3727d9b6639f0c9ceb28852073857c49
```

门禁：

```text
Base Business Partner CI  33060645733 success
Repository Hygiene CI     33060645573 success
Scheduler Reliability CI  33060645802 success
```

Scheduler Reliability 同时验证：

```text
base-service       SUCCESS
fi-service         SUCCESS
scheduler-service  SUCCESS
```

## 11. 当前边界

P2-IMP-01 v1 已建立稳定客商身份，但暂未实现：

- BusinessPartner 附属档案维护页面
- Customer Credit Policy
- 自动历史采购数据 backfill
- 第三方主数据同步
- 法人合并审批流程
- CRM Lead / Opportunity

这些不阻塞 P2-IMP-02。

## 12. 下一阶段

```text
P2-IMP-02 Lead / Opportunity
```

要求：

- Lead 可以在尚未形成 Customer 时存在。
- Opportunity 必须关联稳定 BusinessPartner(CUSTOMER)。
- 商机状态/阶段不产生财务结果。
- 关键经营事实通过 ERP Outbox 发布。
