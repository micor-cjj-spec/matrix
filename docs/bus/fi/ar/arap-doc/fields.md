# 字段设计

## 主表字段

| 字段 | 含义 | 类型 | 是否必填 | 备注 |
|---|---|---|---|---|
| fid | 主键 | Long | 是 | 表主键 |
| fdoctype | 单据类型 | string | 是 | 支持 AR/AP 及其细分类型fileciteturn167file0L24-L35 |
| fnumber | 单据号 | string | 否 | 未传时系统自动生成 `doctype-timestamp`fileciteturn167file0L43-L48 |
| fdate | 单据日期 | date | 是 | 为空时默认当天fileciteturn167file0L417-L419 |
| fcounterparty | 往来方 | string | 是 | AR/AP 共用 |
| famount | 金额 | decimal | 是 | 必须大于 0fileciteturn167file0L418-L420 |
| fstatus | 状态 | string | 是 | DRAFT / SUBMITTED / AUDITED / REJECTEDfileciteturn167file0L24-L27 |
| fremark | 备注 | string | 否 | |
| fpayMethod | 付款方式 | string | 否 | 付款申请差异字段fileciteturn162file0L24-L27 |
| fplannedPayDate | 计划付款日期 | date | 否 | 付款申请差异字段fileciteturn162file0L24-L27 |
| fsettleMethod | 结算方式 | string | 否 | 结算处理差异字段fileciteturn162file0L27-L29 |
| fwriteoffDetail | 核销明细 | string | 否 | 结算处理差异字段fileciteturn162file0L27-L29 |
| fsourceBillNo | 来源单号 | string | 否 | 暂估单差异字段fileciteturn162file0L30-L31 |
| fvoucherId | 联动凭证ID | Long | 否 | 自动生成凭证后回写fileciteturn162file0L32-L34 |
| fvoucherNumber | 联动凭证号 | string | 否 | 自动生成凭证后回写fileciteturn162file0L32-L34 |
| fauditedBy | 审核人 | string | 否 | |
| fauditedTime | 审核时间 | datetime | 否 | |

## 明细字段

当前代码实体为单表模型，未显式拆分明细表；差异字段直接挂在主表中承接不同单据类型。fileciteturn162file0L1-L36
