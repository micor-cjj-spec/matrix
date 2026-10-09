# 字段设计

## 查询参数
同应付单据查询，固定 `docType=AP_PAYMENT_PROCESS`。

## 主体字段
当前付款处理单据复用应付公共字段：
| 字段 | 含义 |
|---|---|
| fid | 主键 |
| fdoctype | 单据类型 |
| fnumber | 单据号 |
| fdate | 单据日期 |
| fcounterparty | 往来方 |
| famount | 金额 |
| fstatus | 状态 |
| fremark | 备注 |
| fvoucherId | 关联凭证ID |
| fvoucherNumber | 关联凭证号 |
