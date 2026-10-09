# P1 采购前置链设计 v1

> 状态：Draft v1  
> 日期：2026-08-27

## 1. 定位

P0 已从 PurchaseOrder 跑通到 Payment Voucher，但业务方案中的采购前置链仍缺失。

业务需求基线明确存在：

```text
采购需求 / 采购申请
→ 采购询比价 / 寻源
→ 采购合同
→ 采购订单
```

Matrix P1 优先补齐这条链，而不是先扩大平台抽象。

## 2. P1 顺序

```text
P1-IMP-01 PurchaseRequest
P1-IMP-02 RFQ / 询价与比价
P1-IMP-03 PurchaseContract
P1-IMP-04 PurchaseRequest / Sourcing / Contract → PurchaseOrder
P1-IMP-05 DeliveryPlan / Supplier Collaboration
P1-IMP-06 Purchase Return / Claim / Deduction
P1-IMP-07 Procurement BOTP Relation Completion
```

税务发票深化在采购主链稳定后进入独立 P1 子阶段。

## 3. 采购申请边界

PurchaseRequest 负责：

```text
需求事实
申请人 / 需求部门
申请类型
采购用途
预算金额
需求日期
项目 / 成本中心
物料 / 数量 / 预计单价
审批结果
执行状态
```

不负责：

```text
审批人路由
询价供应商选择
报价评分
合同条款
采购订单价格最终确认
```

审批人路由属于 workflow-service。

## 4. 状态拆分

```text
Lifecycle:
DRAFT / EFFECTIVE / CANCELLED

Approval:
DRAFT / SUBMITTED / APPROVED / REJECTED

Execution:
NONE / SOURCING / CONTRACTING / ORDERING / COMPLETE
```

三套状态不合并。

## 5. Business Event

最终审批通过同事务产生：

```text
PURCHASE_REQUEST_APPROVED
routingKey:
biz.procurement.purchase_request.approved
```

后续 RFQ / Sourcing 以该业务事实作为可选触发入口。

## 6. Workflow 边界

P1-IMP-01 不硬编码业务方案中的金额阈值、部门类型和领导层级。

采购服务只提供：

```text
submit
approval-result(APPROVED / REJECTED)
```

workflow-service 后续负责：

```text
definition
routing
task
approval timeline
callback
```

这样业务审批规则变化不会要求修改采购领域代码。

## 7. 下游数量

PurchaseRequestEntry 预留：

```text
sourcedQuantity
orderedQuantity
```

后续由 Sourcing / Order 执行链通过事务与 BOTP/Relation 反写，不允许前端直接作为权威量修改。
