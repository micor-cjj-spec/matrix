# P1-IMP-01 PurchaseRequest 实现记录

> 状态：Implemented v1  
> 日期：2026-08-27  
> 目标分支：dev

## 1. 目标

补齐 P0 P2P 之前缺失的采购申请领域，为后续询比价、采购合同和采购订单衔接提供正式上游业务对象。

## 2. Schema

新增：

```text
matrix_erp_purchase_request
matrix_erp_purchase_request_entry
```

交付 SQL：

```text
deliverables/erp/004-purchase-request/schema.sql
```

所有新增对象遵循 Matrix 数据库命名规范。

## 3. Header

主表核心字段：

```text
tenant / org
number / date
requester
requestDepartment
requestType
purpose
currency
budgetAmount
totalQuantity
estimatedAmount
requiredDate
project / costCenter
sourceDocument
status
approvalStatus
executionStatus
workflowInstanceId
rejectReason
approvedBy / approvedTime
```

## 4. Entry

分录核心字段：

```text
material
quantity
estimatedUnitPrice
estimatedAmount
requiredDate
project
costCenter
sourcedQuantity
orderedQuantity
```

创建和修改时由后端统一计算 estimatedAmount。

## 5. State

```text
Lifecycle:
DRAFT → EFFECTIVE
  ↘ CANCELLED

Approval:
DRAFT → SUBMITTED → APPROVED
                  ↘ REJECTED

Execution:
NONE → SOURCING → CONTRACTING → ORDERING → COMPLETE
```

P1-IMP-01 只直接维护 NONE；后续阶段负责执行状态推进。

## 6. Approval

接口不包含固定审批金额阈值。

```text
POST /procurement/purchase-requests/{fid}/submit
POST /procurement/purchase-requests/{fid}/approval-result
```

approval-result 仅支持：

```text
APPROVED
REJECTED
```

可接 workflow-service 最终回写。

## 7. Business Event

APPROVED 与状态更新处于同一 ERP 本地事务。

事件：

```text
PURCHASE_REQUEST_APPROVED
biz.procurement.purchase_request.approved
```

payload 包含申请头、预算、项目/成本中心以及申请行快照。

## 8. API

```text
POST   /procurement/purchase-requests
PUT    /procurement/purchase-requests/{fid}
GET    /procurement/purchase-requests/{fid}
GET    /procurement/purchase-requests
POST   /procurement/purchase-requests/{fid}/submit
POST   /procurement/purchase-requests/{fid}/approval-result
POST   /procurement/purchase-requests/{fid}/cancel
DELETE /procurement/purchase-requests/{fid}
```

## 9. Invariants

- 草稿/驳回才允许修改和删除。
- 只有 SUBMITTED 才接受审批结果。
- 审批通过才进入 EFFECTIVE。
- 已进入采购执行的申请不能直接取消。
- 审批中的申请不能直接取消。
- 采购执行量与申请量分离记录。
- 采购申请不决定审批路由。

## 10. Tests

新增：

```text
PurchaseRequestServiceTest
```

覆盖：

1. 创建时总数量和预计金额计算。
2. 默认 DRAFT / DRAFT / NONE。
3. APPROVED 后 EFFECTIVE。
4. workflowInstanceId 回写。
5. APPROVED 同事务发送 PURCHASE_REQUEST_APPROVED。
6. SOURCING 状态不能直接取消。

## 11. Commit / CI

实现提交：

```text
6ea9dcf4fe8741297279f6ca5f3274717727df9b
feat(procurement): add purchase request domain
```

CI：

```text
Biz Finance P0 CI      33044686309 success
Repository Hygiene CI 33044686313 success
```

虽然 workflow 名仍为 P0，它执行 erp-service / fi-service / botp-service Maven 测试，因此继续作为当前采购后端门禁。

## 12. 下一阶段

```text
P1-IMP-02 RFQ / 采购询价与比价
```

目标是把 APPROVED PurchaseRequest 转为可执行寻源需求，支持多供应商报价、比价结果和定标事实；不在 PurchaseRequest 内塞供应商报价字段。
