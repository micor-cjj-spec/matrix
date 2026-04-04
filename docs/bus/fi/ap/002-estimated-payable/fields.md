# 字段设计

## 查询参数
同应付单据查询，固定 `docType=AP_ESTIMATE`。

## 主体字段
除应付公共字段外，暂估应付额外关注：
| 字段 | 含义 |
|---|---|
| fsourceBillNo | 暂估来源单号 |

## 联动字段
| 字段 | 含义 |
|---|---|
| fvoucherId | 关联凭证ID |
| fvoucherNumber | 关联凭证号 |
