# P2-IMP-03 报价 / 投标报价 + 销售合同（开发首批）

> 来源分支：agent/p2-imp-03-security-routing-20261009，目标：dev。
> 当前状态：代码实现待 CI 验证；不等同于部署验收通过。

## 范围
- ERP 域持久化 `SalesQuote`、`SalesQuoteEntry`、`SalesContract`、`SalesContractEntry`。
- 引用同租户商机，调用 BusinessPartner CUSTOMER Gate；报价客户不能脱离商机。
- 支持 `QUOTE` 普通报价与 `TENDER` 带招标编号的投标报价；**尚未实现招投标公告、评标、定标工作流**。
- 金额由后端以 `quantity × unitPrice` 和税率重新计算，不信任前端金额。
- 报价状态：DRAFT → SUBMITTED → APPROVED → SENT → ACCEPTED / REJECTED；SUBMITTED 可撤回 DRAFT，DRAFT 可作废 CANCELLED，SENT 超过有效期可显式标记 EXPIRED。过期报价不能发送或接受。
- DRAFT 支持修改报价有效期/收付款条款和 1~100 行明细；只能由服务端重新计算并在同一事务替换草稿明细，已审核报价不可修改。
- 报价 ACCEPTED 后才能创建合同；一个报价只能创建一个合同；合同明细快照继承报价分录。
- 合同审批：DRAFT → SUBMITTED → APPROVED；审核后生命周期 EFFECTIVE。
- 复用 ERP Outbox 发布 `SALES_QUOTE_ACCEPTED` 与 `SALES_CONTRACT_EFFECTIVE`，不创建 AR / Voucher。

## SQL
部署时先执行 `deliverables/erp/012-sales-quotation-contract/schema.sql` 到 `matrix_erp`，其后启动含新模块的 ERP 服务。

## 功能开关与授权约束

销售模块 Controller 默认关闭。完成安全验收、数据库脚本及灰度验收后，在 ERP 受控配置中显式设置 `matrix.sales.commercial-enabled=true`。仅开启开关并不等于完成授权审查。

- 请求必须有 `Authorization: Bearer <JWT>`，服务端使用与 auth-service **一致的** `security.jwt.secret` 校验签名及有效期，不能使用任意代理的用户身份请求头。
- `tenantId` 必须与签名 JWT 的 `tenantId` 一致。历史 JWT 不含租户声明时仅允许 `default` 租户。
- `orgId` 为 **必填**，且应在 JWT 的 `organizationIds` 之内；单据详情及变更也检查单据所属组织。未经签名的组织与操作人头一律不采信。
- 只读角色：`SALES_VIEWER`、`SALES_EDITOR`、`SALES_APPROVER`、`SALES_ADMIN` 或 `ADMIN`。
- 草稿创建、提交、发送：`SALES_EDITOR`、`SALES_ADMIN` 或 `ADMIN`。
- 审批、客户接受/拒绝：`SALES_APPROVER`、`SALES_ADMIN` 或 `ADMIN`；后续还需将客户接受动作关联客户确认凭据及 Workflow 实例。
- 服务端从签名 JWT 的 `id` 获取审计操作人，不再采信客户端 `X-Operator-Id`。
- **现有 auth-service 的 JWT 签发代码仅包含身份、组织等信息，未签发上述销售角色。** 在完成可信角色签发或权限服务集成前，新接口会拒绝现有无角色的令牌；保持功能关闭，不以默认放行绕过。
- JWT 的角色/组织快照可能变旧，仍应在正式上线前增加实时授权校验、撤权生效测试，并确保 ERP 端口不能绕过受控入口直接暴露。


## 登录与角色签发（第二轮开发）

- 新增 `deliverables/auth/001-sales-role-grant/schema.sql`。必须由 DBA 在 auth-service 所连接的受控数据库执行；使用 `matrix_auth_sales_role_grant` 持久化 `ftenant_id / forg_id / fuser_id / frole_code`。**没有任何用户或管理员默认获权**。
- `auth-service` 的 `matrix.sales-auth.issuer-enabled` 默认 `false`。手工完成授权数据核对后，指定 `matrix.sales-auth.tenant-id` 并明确开启，登录时才从 DB 按 **用户 + 租户 + 组织**读取 ACTIVE 授权，将允许的 SALES_* 角色签入 JWT。
- 当前组织映射沿用旧登录代码的 `BizfiBaseUser.ftid → organizationIds`，并不能据此证明 `ftid` 一定是业务组织；生产开放前必须核验映射与 `erp-service.forg_id` 一致。
- 新增 ERP 每次销售权限校验 Redis `token:<jwt>` 中的当前用户 ID；Redis 不可用 / 会话被清除时拒绝访问（必须确认 auth 和 ERP 连接同一 Redis，并保留旧 `RedisTemplate` 序列化机制）。
- **授权表变更不会自动清除已签发的 JWT**。紧急撤权需同步失效受影响的 Redis 会话（不可只更新 SQL）。角色变更立即生效的自动撤权流程尚未开发；无自动联动前禁止正式启用。
- 不得通过请求参数、前端缓存或自行编辑 JWT 获取销售角色。角色授权及删除只能由受控管理员流程完成；没有自动赋权脚本。

## API
```
POST /sales/quotes
PUT  /sales/quotes/{id}              (仅 DRAFT)
GET  /sales/quotes?tenantId=...&orgId=...
GET  /sales/quotes/{id}?tenantId=...
POST /sales/quotes/{id}/{submit|approve|send|accept|reject|withdraw|cancel|expire}?tenantId=...
POST /sales/contracts
GET  /sales/contracts?tenantId=...&orgId=...
GET  /sales/contracts/{id}?tenantId=...
POST /sales/contracts/{id}/{submit|approve}?tenantId=...
```

## 边界
- 已添加签名 JWT / 角色 / 组织检查，但尚未接入正式 Workflow 和实时授权系统；**不得仅凭静态 JWT 角色完成生产权限验收**。
- 客户接受/拒绝当前是受审批角色限制的内网操作，不等价于第三方客户电子签署。
- Quote → SalesContract 的权威引用是 `fquote_id`。后续 P2-IMP-08 才接入统一 BOTP Relation/Entry Relation。
- 下一批：定时过期扫描、正式招投标业务、CRM 前端、Workflow 审批与实时撤权、合同到销售订单的 BOTP 映射。

