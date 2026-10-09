# P0-IMP-01 `erp-service` + PurchaseOrder 实现记录

> 状态：Implemented v1  
> 日期：2026-08-26

## 1. 本轮范围

本轮只落地 P0-07 的第一张真实经营单据：采购订单。

已实现：

```text
root Maven reactor
  ↓
erp-service
  ↓
PurchaseOrder + PurchaseOrderEntry
  ↓
CRUD
  ↓
submit / audit / reject / cancel
```

暂不实现：

```text
PurchaseReceipt
PurchaseAcceptance
PurchaseInbound
SupplierInvoice
BOTP PO → Receipt 转换
Business Event
Accounting Event
前端页面
```

这些进入后续 P0-IMP-02 及之后阶段。

## 2. 新增模块

```text
erp-service
├─ pom.xml
├─ src/main/java/single/cjj/erp/ErpApplication.java
├─ src/main/java/single/cjj/erp/config/MybatisPlusConfig.java
├─ src/main/java/single/cjj/erp/controller/ErpExceptionHandler.java
└─ src/main/java/single/cjj/erp/procurement/order
   ├─ controller/PurchaseOrderController.java
   ├─ dto/PurchaseOrderContracts.java
   ├─ entity/PurchaseOrderEntity.java
   ├─ entity/PurchaseOrderEntryEntity.java
   ├─ mapper/PurchaseOrderMapper.java
   ├─ mapper/PurchaseOrderEntryMapper.java
   └─ service/PurchaseOrderService.java
```

根 `pom.xml` 已注册：

```xml
<module>erp-service</module>
```

## 3. 数据库对象

DDL：

```text
deliverables/erp/001-purchase-order/schema.sql
```

数据库：

```text
matrix_erp
```

表：

```text
matrix_erp_purchase_order
matrix_erp_purchase_order_entry
```

全部新字段遵循 Matrix 数据库命名规范：字段以 `f` 开头，主键为 `fid`，包含租户、组织、审计、逻辑删除和乐观锁字段。

## 4. 状态机

生命周期：

```text
DRAFT
  ↓ audit
EFFECTIVE
  ↓ future fulfillment
COMPLETED / CLOSED
```

当前实现审批状态：

```text
DRAFT
  ↓ submit
SUBMITTED
  ├─ audit  → AUDITED + fstatus=EFFECTIVE
  └─ reject → REJECTED + fstatus=DRAFT
```

取消：

```text
EFFECTIVE + AUDITED + receiptStatus=NONE
→ CANCELLED
```

已发生收货后禁止直接取消采购订单，后续必须通过退货/关闭流程处理。

## 5. 数据权威规则

### 5.1 供应商

采购订单保存：

```text
fbusiness_partner_id    权威关联
fbusiness_partner_code  交易快照
fbusiness_partner_name  交易快照
```

本轮尚未接入 P0-01 BusinessPartner 持久化实现，因此暂时只校验 ID/Code/Name 必填，不跨服务校验 SUPPLIER Role。待 BusinessPartner 实现后补充服务间校验。

### 5.2 金额

前端不提交主表总额作为权威值。

后端按分录计算：

```text
lineNet   = quantity × unitPrice
lineTax   = lineNet × taxRate
lineGross = lineNet + lineTax
```

主表：

```text
ftotal_quantity
fnet_amount
ftax_amount
fgross_amount
```

由所有分录汇总得出。

当前税率 Contract 约定为 `0~1` 小数，例如 `0.13` 表示 13%。

## 6. 累计履约字段

采购订单分录初始化：

```text
freceived_quantity = 0
faccepted_quantity = 0
finbound_quantity = 0
finvoiced_quantity = 0
fsettled_amount = 0
```

当前不允许客户端维护这些字段。

P0-IMP-02 起由 BOTP Relation + 采购领域反写重算，避免客户端直接篡改累计履约结果。

## 7. API

服务 context-path：

```text
/api
```

资源：

```text
POST   /api/procurement/purchase-orders
PUT    /api/procurement/purchase-orders/{fid}
GET    /api/procurement/purchase-orders/{fid}?tenantId=...
GET    /api/procurement/purchase-orders?tenantId=...
DELETE /api/procurement/purchase-orders/{fid}?tenantId=...

POST /api/procurement/purchase-orders/{fid}/submit?tenantId=...
POST /api/procurement/purchase-orders/{fid}/audit?tenantId=...
POST /api/procurement/purchase-orders/{fid}/reject?tenantId=...
POST /api/procurement/purchase-orders/{fid}/cancel?tenantId=...
```

写操作支持可选：

```text
X-User-Id
```

用于审计字段。

## 8. 并发与隔离

- 所有读取和状态动作要求 `tenantId`。
- 订单号在租户内唯一。
- Entity 使用 `@Version`。
- `erp-service` 注册 `OptimisticLockerInnerInterceptor`。
- 状态变更更新失败时提示刷新重试。
- 草稿明细替换和主表更新处于一个本地事务。

## 9. 启动配置

`erp-service`：

```text
spring.application.name = erp-service
context-path = /api
DB = matrix_erp
```

支持环境变量：

```text
ERP_DB_URL
ERP_DB_USERNAME
ERP_DB_PASSWORD
NACOS_ADDR
NACOS_GROUP
NACOS_NAMESPACE
```

## 10. 已知待办

P0-IMP-01 有意不解决：

1. BusinessPartner SUPPLIER Role 的远程权威校验。
2. Workflow Service 正式审批流集成；当前先使用领域状态机接口。
3. PO → Receipt BOTP Adapter / Rule。
4. PurchaseOrder 履约完成、关闭规则。
5. Gateway Nacos 路由配置。
6. 前端采购订单页面。
7. Business Event Outbox。

## 11. 下一步

P0-IMP-02：

```text
PurchaseOrder
→ PurchaseReceipt
→ PurchaseAcceptance
→ PurchaseInbound
```

重点实现：

- 三类单据主子表。
- PO → Receipt 的 BOTP 分录数量关系。
- reservation 防并发超收。
- 收货累计数量反写。
- Receipt → Acceptance → Inbound 的单据链。
- `PURCHASE_INBOUND_CONFIRMED` 作为第一条真正的财务触发 Business Event。
