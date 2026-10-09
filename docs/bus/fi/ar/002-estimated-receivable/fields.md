# 字段设计

## 查询参数
同应收单据查询，固定 `docType=AR_ESTIMATE`。

## 主体字段
除应收公共字段外，暂估应收额外关注：
| 字段 | 含义 |
|---|---|
| fsourceBillNo | 暂估来源单号 |

## 联动字段
| 字段 | 含义 |
|---|---|
| fvoucherId | 关联凭证ID |
| fvoucherNumber | 关联凭证号 |
