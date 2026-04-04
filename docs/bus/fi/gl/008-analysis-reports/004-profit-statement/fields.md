# 字段设计

## 查询参数
| 参数 | 含义 | 说明 |
|---|---|---|
| orgId | 业务单元 | 可空 |
| startPeriod | 开始期间 | `yyyy-MM` |
| endPeriod | 结束期间 | `yyyy-MM` |
| currency | 币种 | 默认 `CNY` |
| templateId | 报表模板ID | 可空 |
| showZero | 显示零值行 | `true / false` |

## 主表行字段
| 字段 | 含义 |
|---|---|
| rowNo | 行次 |
| itemCode | 项目编码 |
| itemName | 项目名称 |
| level | 层级 |
| currentAmount | 本期金额 |
| ytdAmount | 本年累计金额 |
