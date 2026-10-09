# P2-IMP-03 报价 / 投标报价 + 销售合同（开发首批）

> 来源分支：agent/p2-imp-03-security-routing-20261009，目标：dev。
> 当前状态：代码实现待 CI 验证；不等同于部署验收通过。

## 范围
- ERP 域持久化 `SalesQuote`、`SalesQuoteEntry`、`SalesContract`、`SalesContractEntry`。
- 引用同租户商机，调用 BusinessPartner CUSTOMER Gate；报价客户不能脱离商机。
- 支持 `QUOTE` 普通报价与 `TENDER` 带招标编号的投标报价；**尚未实现招投标公告、评标、定标工作流**。
- 金额由后端以 `quantity × unitPrice` 和税率重新计算，不信任前端金额。
- 报价状态：DRAFT → SUBMITTED → APPROVED → SENT → ACCEPTED / REJECTED；过期报价不能接受。
- 报价 ACCEPTED 后才能创建合同；一个报价只能创建一个合同；合同明细快照继承报价分录。
- 合同审批：DRAFT → SUBMITTED → APPROVED；审核后生命周期 EFFECTIVE。
- 复用 ERP Outbox 发布 `SALES_QUOTE_ACCEPTED` 与 `SALES_CONTRACT_EFFECTIVE`，不创建 AR / Voucher。

## SQL
部署时先执行 `deliverables/erp/012-sales-quotation-contract/schema.sql` 到 `matrix_erp`，其后启动含新模块的 ERP 服务。

## API
```
POST /sales/quotes
GET  /sales/quotes?tenantId=...
GET  /sales/quotes/{id}?tenantId=...
POST /sales/quotes/{id}/{submit|approve|send|accept|reject}?tenantId=...
POST /sales/contracts
GET  /sales/contracts?tenantId=...
GET  /sales/contracts/{id}?tenantId=...
POST /sales/contracts/{id}/{submit|approve}?tenantId=...
```

## 边界
- 未接入正式 Workflow 审批授权；服务端须由网关与权限中间件限制审批动作，未验证前不可开放给普通用户。
- 业务处理中的 `X-Operator-Id` 仅审计标识，不是可信授权凭证。
- Quote → SalesContract 的权威引用是 `fquote_id`。后续 P2-IMP-08 才接入统一 BOTP Relation/Entry Relation。
- 下一批：报价修改/撤回、正式招投标业务、审批角色与审计、CRM 前端、合同到销售订单的 BOTP 映射。

