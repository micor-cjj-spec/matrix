# 字段设计

## 查询参数
| 参数 | 含义 | 说明 |
|---|---|---|
| orgId | 业务单元 | 可空 |
| period | 期间 | `yyyy-MM` |
| currency | 币种 | 默认 `CNY` |
| templateId | 报表模板ID | 可空 |
| showZero | 显示零值行 | `true / false` |

## 主表行字段
| 字段 | 含义 |
|---|---|
| itemId | 报表项目ID |
| itemCode | 项目编码 |
| itemName | 项目名称 |
| rowNo | 行次 |
| level | 层级 |
| lineType | 行类型 |
| amount | 期末余额 |
| beginAmount | 年初余额 |
| drillable | 是否可下钻 |

## 下钻行字段
| 字段 | 含义 |
|---|---|
| accountId | 科目ID |
| accountCode | 科目编码 |
| accountName | 科目名称 |
| accountType | 科目类型 |
| direction | 余额方向 |
| beginAmount | 年初余额 |
| endAmount | 期末余额 |
| mappingSource | 映射来源 |
