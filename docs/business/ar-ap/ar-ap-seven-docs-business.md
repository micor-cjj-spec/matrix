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

## 7. 当前代码实现对齐（2026-03-12）
已与代码对齐确认：
- 后端统一接口：`/api/arap-doc/*`（create/update/delete/submit/audit/reject/list/detail）
- 前端统一页面：`ArapDocView.vue`，通过路由 `docType` 承载 7 类单据
- 已接入路由：
  - AP/AP_ESTIMATE/AP_PAYMENT_APPLY/AP_PAYMENT_PROCESS
  - AR/AR_ESTIMATE/AR_SETTLEMENT
- 状态流转已生效：`DRAFT -> SUBMITTED -> AUDITED`，支持 `REJECTED`

## 8. 代码与业务文档差异（待对齐项）
以下已在业务文档中定义，但代码尚未全部落地：
1. 单据差异化字段（目前仍是统一字段）
2. 单据联动凭证（审核后自动/手动生成凭证）
3. 列表高级筛选与导出（日期区间、金额区间、导出Excel）

## 9. 对齐策略
- 文档以“当前代码事实 + 待对齐清单”双轨维护。
- 待对齐项每完成一项，更新本文件并在对应提交说明标注。
