# 字段设计

## 查询参数
同应收单据查询，固定 `docType=AR_SETTLEMENT`。

## 主体字段
除应收公共字段外，结算处理额外关注：
| 字段 | 含义 |
|---|---|
| fsettleMethod | 结算方式 |
| fwriteoffDetail | 核销明细 |

## 联动字段
| 字段 | 含义 |
|---|---|
| fvoucherId | 关联凭证ID |
| fvoucherNumber | 关联凭证号 |
