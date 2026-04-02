# 往来单业务说明

## 1. 业务名称
往来单（AR/AP Doc）

## 2. 业务背景
往来单用于统一承接应收、应付以及相关暂估、结算、付款申请、付款处理等往来业务。当前代码中通过 `fdoctype` 区分不同业务类型，包括 `AR`、`AR_ESTIMATE`、`AR_SETTLEMENT`、`AP`、`AP_ESTIMATE`、`AP_PAYMENT_APPLY`、`AP_PAYMENT_PROCESS` 等。fileciteturn167file0L24-L35

## 3. 业务目标
- 用统一模型承载 AR/AP 往来业务
- 支撑草稿、提交、审核、驳回等单据流转
- 支撑审核通过后自动生成并关联总账凭证fileciteturn166file0L13-L27 fileciteturn167file0L267-L319
- 支撑账龄汇总、信用预警、按凭证反查单据等分析能力fileciteturn166file0L29-L37

## 4. 单据定位
往来单属于 `fi/ar` 域中代码已实现的核心业务对象。虽然目录位于 `ar`，但从代码看它同时承载 AR 与 AP 两类往来单据，并通过 `docTypeRoot` 和 `fdoctype` 区分收付方向与业务细分。fileciteturn165file0L12-L22 fileciteturn167file0L421-L426

## 5. 核心业务内容
- 创建、修改、删除草稿单据fileciteturn166file0L11-L18
- 提交、审核、驳回单据fileciteturn164file0L24-L48
- 审核通过后自动生成凭证并回写 `fvoucherId`、`fvoucherNumber`fileciteturn167file0L332-L346
- 提供按单据类型、状态、往来方、日期、金额的分页查询fileciteturn164file0L58-L68
- 提供 AR/AP 维度账龄汇总和信用预警fileciteturn164file0L81-L99

## 6. 上下游关系
### 上游
- 业务来源单据或结算信息（通过 `fsourceBillNo` 等字段承接）fileciteturn162file0L18-L29
- 往来方信息（`fcounterparty`）fileciteturn162file0L18-L24
- 信用配置（`BizfiFiCounterpartyCredit`）fileciteturn165file0L1-L22

### 下游
- 总账凭证与凭证分录fileciteturn167file0L14-L17
- 账龄分析
- 信用预警

## 7. 关键控制点
- 仅草稿可删除；仅草稿/驳回可编辑和提交；仅已提交可审核/驳回fileciteturn167file0L54-L61 fileciteturn167file0L349-L370
- 金额必须大于 0，往来方与单据类型不能为空fileciteturn167file0L414-L420
- 提交或审核时可能被信用规则硬拦截fileciteturn167file0L372-L413
- 仅已审核单据可生成凭证fileciteturn167file0L271-L280
