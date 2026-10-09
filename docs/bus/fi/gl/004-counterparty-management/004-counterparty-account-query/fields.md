# 字段设计

## 查询参数
| 参数 | 含义 | 说明 |
|---|---|---|
| docTypeRoot | 往来类型 | `AR / AP` |
| counterparty | 往来方 | 可空 |
| docType | 单据类型 | 可空 |
| status | 单据状态 | 可空 |
| openOnly | 仅看未核销 | 默认 `false` |
| keyword | 关键字 | 匹配单据号、往来方、单据类型、状态、凭证号、备注 |
| asOfDate | 统计日期 | 可空，默认当天 |

## 返回结构
返回 `Map<String,Object>`，核心字段包括：
- `docCount`
- `totalAmount`
- `writtenOffAmount`
- `openAmount`
- `openDocCount`
- `records`
- `warnings`

## 查询结果行
与往来对账单单据行口径一致，核心字段包括：
- `fid`
- `counterparty`
- `docType`
- `number`
- `docDate`
- `status`
- `role`
- `amount`
- `writtenOffAmount`
- `openAmount`
- `writeoffStatus`
- `ageDays`
- `voucherNumber`
- `remark`
