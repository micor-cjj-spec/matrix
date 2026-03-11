# 应收应付七类单据业务设计（MVP）

> 模块：AR/AP
> 更新时间：2026-03-12

## 1. 七类单据范围
### 应付（4）
1. AP：应付单
2. AP_ESTIMATE：暂估应付
3. AP_PAYMENT_APPLY：付款申请
4. AP_PAYMENT_PROCESS：付款处理

### 应收（3）
5. AR：应收单
6. AR_ESTIMATE：暂估应收
7. AR_SETTLEMENT：结算处理

## 2. 统一状态机
`DRAFT -> SUBMITTED -> AUDITED`
支持 `REJECTED` 驳回回到可编辑/可提交。

## 3. 通用字段（MVP）
- 单据类型 `fdoctype`
- 单据号 `fnumber`
- 单据日期 `fdate`
- 往来方 `fcounterparty`
- 金额 `famount`
- 状态 `fstatus`
- 备注 `fremark`
- 审核人/审核时间

## 4. 关键规则
1. 仅草稿/驳回可编辑。
2. 仅草稿可删除。
3. 仅已提交可审核/驳回。
4. 金额必须 > 0。
5. 往来方必填。

## 5. 前端交互（七类统一）
- 顶部按钮：创建、编辑、提交、审核、驳回、删除
- 列表点击行选中后执行动作
- 页面复用一个组件，通过路由 `docType` 区分

## 6. 后端接口（统一）
- `GET /arap-doc/list?docType=...`
- `POST /arap-doc`
- `PUT /arap-doc`
- `DELETE /arap-doc/{fid}`
- `POST /arap-doc/submit/{fid}`
- `POST /arap-doc/audit/{fid}`
- `POST /arap-doc/reject/{fid}`

## 7. 落地说明
当前先实现“七类单据统一MVP流程”，后续可按单据类型扩展专属字段和联动规则（如付款申请关联资金计划、结算处理关联核销明细）。
