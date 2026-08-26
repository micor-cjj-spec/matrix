# P0-01 BusinessPartner 统一客商中心设计 v1

> 状态：Draft v1  
> 归档日期：2026-08-26

## 1. 设计目标

客户和供应商统一底层主体，角色分离：

```text
BusinessPartner
  ├─ CUSTOMER
  ├─ SUPPLIER
  ├─ CUSTOMER + SUPPLIER
  ├─ FINANCIAL_INSTITUTION
  └─ OTHER
```

同一法人只维护一份主体信息，CRM/SRM 分别维护客户/供应商业务角色。

## 2. 核心表

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

## 3. 主体与角色分离

`fpartner_type` 表达主体类型，例如：

```text
ORGANIZATION
PERSON
```

CUSTOMER/SUPPLIER 不放在主体类型中，而由 `matrix_base_business_partner_role` 表达。

角色类型首批：

```text
CUSTOMER
SUPPLIER
FINANCIAL_INSTITUTION
LOGISTICS_PROVIDER
SERVICE_PROVIDER
OTHER
```

一个 Partner 可以同时有 CUSTOMER 和 SUPPLIER 角色，但同一个 Partner 同一角色不得重复。

## 4. 状态分离

不要再用单一 `fstatus=AUDITED` 同时表达审批和生命周期。

主体生命周期：

```text
DRAFT
ACTIVE
DISABLED
CLOSED
```

审批：

```text
DRAFT
SUBMITTED
AUDITED
REJECTED
```

典型：

```text
创建      DRAFT / DRAFT
提交      DRAFT / SUBMITTED
审核通过  ACTIVE / AUDITED
驳回      DRAFT / REJECTED
```

## 5. 组织可用范围

`matrix_base_business_partner_org` 用来表达一个集团级客商在哪些组织可用，而不是每个组织复制一份客商。

典型字段：

```text
fid
ftenant_id
fbusiness_partner_id
forg_id
fcustomer_enabled
fsupplier_enabled
fstatus
feffective_date
fexpiry_date
```

## 6. Settlement 必须按角色

同一 Partner 作为客户和供应商时，收款账期与付款账期可以不同，所以结算档案必须关联 `fbusiness_partner_role_id`。

建议字段：

```text
fsettlement_type
fpayment_term_code
fsettlement_currency_id
fsettlement_method
fpayment_method
fcredit_days
fadvance_required
```

客户信用额度不放入基础主数据，进入后续 CRM/Credit 领域。

## 7. 银行与税务档案

银行账户独立表：

```text
faccount_name
fbank_code
fbank_name
fbank_branch
faccount_no
fcurrency_id
fusage_type
fis_default
fverification_status
```

`fusage_type`：

```text
RECEIPT
PAYMENT
BOTH
```

TaxProfile 只保存客商税务主数据，不承载发票认证、抵扣、纳税申报等税务交易能力。

## 8. Java 包结构

```text
base-service/src/main/java/single/cjj/bizfi/partner
├─ controller
│  ├─ BusinessPartnerController
│  ├─ CustomerController
│  └─ SupplierController
├─ dto
├─ entity
├─ mapper
├─ service
│  ├─ BusinessPartnerService
│  ├─ PartnerRoleService
│  ├─ CustomerFacadeService
│  └─ SupplierFacadeService
└─ model
```

## 9. 现有 Customer/Supplier API 兼容

第一阶段保留：

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

内部改为：

```text
CustomerController
→ CustomerFacadeService
→ BusinessPartnerService
→ CUSTOMER Role
```

现有 `BaseDataWorkflowController` 中 `/customer/**`、`/supplier/**` 需要移除，避免 Spring Ambiguous Mapping；其余基础资料可暂时保留原实现。

## 10. 去重策略

自动识别优先级：

```text
统一社会信用代码
→ sourceSystem + sourceId
→ 人工确认
```

不能按公司名称相似直接自动合并。

## 11. AR/AP 迁移

历史 AR/AP 的 `fcounterparty` 字符串逐步迁移为：

```text
fbusiness_partner_id   权威关联
fbusiness_partner_code 历史快照
fbusiness_partner_name 历史快照
```

交易单据必须保存 Reference ID + Snapshot，避免主数据改名后历史单据语义变化。

## 12. 验收

- 创建客户：Partner + CUSTOMER Role。
- 创建供应商：Partner + SUPPLIER Role。
- 同主体双角色：仅一个 Partner。
- 旧客户/供应商页面接口不变。
- 服务重启后数据持久化，不再依赖内存 Map。
- 已审核资料不能通过旧接口直接删除。
