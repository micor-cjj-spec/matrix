# P1-IMP-03 PurchaseContract 实现记录

> 状态：Implemented v1  
> 日期：2026-08-27  
> 目标分支：dev

## 1. 目标

承接已确认 SourcingAward，将采购寻源定标事实沉淀为正式采购合同对象，为后续 Contract → PurchaseOrder 提供稳定上游来源。

正式链路：

```text
PurchaseRequest
→ RFQ / SupplierQuote
→ SourcingAward
→ PurchaseContract
→ PurchaseOrder
```

P1-IMP-03 只负责合同事实，不提前生成 PurchaseOrder。

## 2. 关键业务边界

SourcingAward v1 可以包含多个中标供应商，因此 PurchaseContract 不直接与整个 Award 强绑定为一对一关系。

正式规则：

```text
1 SourcingAward
→ N PurchaseContract
```

但：

```text
1 PurchaseContract
→ 1 Supplier
```

同一合同中如果混入不同 Business Partner 的 AwardEntry，后端直接拒绝。

## 3. Schema

新增：

```text
matrix_erp_purchase_contract
matrix_erp_purchase_contract_entry
```

交付 SQL：

```text
deliverables/erp/006-purchase-contract/schema.sql
```

全部遵守 Matrix 数据库命名规范。

## 4. Contract Header

核心字段：

```text
tenant / org
number / date / title
sourcingAwardId
businessPartner
currency
startDate / endDate
paymentTermCode
deliveryTermCode
totalQuantity
netAmount / taxAmount / grossAmount
status
approvalStatus
executionStatus
workflowInstanceId
rejectReason
approvedBy / approvedTime
remark
```

状态拆分：

```text
Lifecycle:
DRAFT / EFFECTIVE

Approval:
DRAFT / SUBMITTED / APPROVED / REJECTED

Execution:
NONE / ORDERING / COMPLETE
```

三套状态不合并。

## 5. Contract Entry

采购合同分录保留完整来源链：

```text
sourcingAwardEntryId
rfqEntryId
purchaseRequestId
purchaseRequestEntryId
```

业务快照：

```text
material
quantity
unitPrice
taxRate
netAmount
taxAmount
grossAmount
plannedDeliveryDate
project
costCenter
orderedQuantity
```

`orderedQuantity` 为 P1-IMP-04 Contract → PurchaseOrder 预留。

## 6. 金额规则

合同价格与税率从已确认定标分录继承，不由前端重新输入权威价格。

后端计算：

```text
netAmount   = quantity × awardedUnitPrice
taxAmount   = netAmount × taxRate
grossAmount = netAmount + taxAmount
```

税率沿用 0-1 小数语义。

## 7. 防重复签约

为 `SourcingAwardEntry` 增加行级锁：

```sql
SELECT ...
FOR UPDATE
```

创建或修改合同前，后端先锁定来源定标分录，再汇总当前其他有效合同已经占用的数量。

约束：

```text
newContractQuantity
<= awardedQuantity - alreadyContractedQuantity
```

因此同一中标数量不能被两个并发合同重复签约。

Draft 合同也视为已占用数量，避免两个草稿同时超签。
删除草稿后对应合同分录逻辑删除，数量自然释放。

## 8. 审批边界

采购合同不硬编码审批人路由和金额阈值。

ERP 仅提供：

```text
submit
approval-result(APPROVED / REJECTED)
```

workflow-service 继续负责审批定义、路由、任务和回调。

审批通过：

```text
approvalStatus = APPROVED
status = EFFECTIVE
```

审批驳回：

```text
approvalStatus = REJECTED
status = DRAFT
```

## 9. Business Event

合同最终审批生效与合同状态更新处于同一 ERP 本地事务。

事件：

```text
PURCHASE_CONTRACT_EFFECTIVE
```

routing key：

```text
biz.procurement.purchase_contract.effective
```

source document：

```text
ERP_PURCHASE_CONTRACT
```

payload 包含：

```text
contract
sourcingAward
supplier
currency
payment/delivery terms
amount
source request
material
quantity
price
tax
planned delivery
```

## 10. API

```text
POST   /procurement/purchase-contracts
PUT    /procurement/purchase-contracts/{fid}
GET    /procurement/purchase-contracts/{fid}
GET    /procurement/purchase-contracts

POST   /procurement/purchase-contracts/{fid}/submit
POST   /procurement/purchase-contracts/{fid}/approval-result

DELETE /procurement/purchase-contracts/{fid}
```

只有草稿或已驳回合同允许修改和删除。

## 11. Tests

新增：

```text
PurchaseContractServiceTest
```

覆盖：

1. 从 Award 快照生成合同并计算金额。
2. 默认继承 RFQ 币种和报价交期。
3. 一个合同混合多个供应商时拒绝。
4. 超过 AwardEntry 剩余可签约数量时拒绝。
5. 合同审批通过后进入 EFFECTIVE。
6. APPROVED 同事务发送 PURCHASE_CONTRACT_EFFECTIVE。
7. 合同起止日期反向时拒绝。

## 12. Commit / CI

实现 PR：

```text
PR #85
feat(procurement): implement P1 purchase contract
```

合并提交：

```text
147dca6787ec540294cc38b1324dc921bda9c307
```

PR CI：

```text
Biz Finance P0 CI      33053173654 success
Repository Hygiene CI 33053173912 success
```

## 13. 当前边界

P1-IMP-03 已完成采购合同后端领域闭环。

本阶段明确不做：

- 合同自动生成 PurchaseOrder
- PurchaseRequest.executionStatus → ORDERING
- ContractEntry.orderedQuantity 回写
- 合同变更版本管理
- 合同终止后的订单释放
- 电子签章
- Supplier Portal
- 完整 BOTP Relation

这些进入后续阶段。

## 14. 下一阶段

```text
P1-IMP-04 PurchaseRequest / Sourcing / Contract → PurchaseOrder
```

目标：

- 从 EFFECTIVE PurchaseContract 正式生成采购订单。
- 采购订单价格/供应商/币种从合同事实继承。
- 回写 ContractEntry.orderedQuantity。
- 回写 PurchaseRequestEntry.orderedQuantity。
- 推进 PurchaseRequest.executionStatus。
- 建立正式上游转换关系，为 P1-IMP-07 BOTP Completion 做准备。
