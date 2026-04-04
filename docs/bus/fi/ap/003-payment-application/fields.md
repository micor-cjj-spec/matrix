# 字段设计

## 查询参数
同应付单据查询，固定 `docType=AP_PAYMENT_APPLY`。

## 主体字段
除应付公共字段外，付款申请额外关注：
| 字段 | 含义 |
|---|---|
| fpayMethod | 付款方式 |
| fplannedPayDate | 预计付款日 |

## 联动字段
| 字段 | 含义 |
|---|---|
| fvoucherId | 关联凭证ID |
| fvoucherNumber | 关联凭证号 |
