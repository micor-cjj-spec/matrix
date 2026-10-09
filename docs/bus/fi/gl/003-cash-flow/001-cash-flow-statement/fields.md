# 字段设计

## 查询参数
| 参数 | 含义 | 说明 |
|---|---|---|
| orgId | 业务单元 | 可空 |
| period | 期间 | 必须为 `yyyy-MM` |
| currency | 币种 | 默认 `CNY` |
| templateId | 模板ID | 可空，空时自动取启用模板 |
| showZero | 显示零值行 | 默认 `true` |

## 返回结构
返回 `ReportQueryResultVO`，核心字段包括：
- `reportType`
- `orgId`
- `period`
- `currency`
- `templateId`
- `templateName`
- `rows`
- `checks`
- `warnings`

## 行字段（ReportRowVO）
| 字段 | 含义 |
|---|---|
| id | 报表项目ID |
| itemCode | 项目编码 |
| itemName | 项目名称 |
| rowNo | 行次 |
| level | 层级 |
| lineType | 行类型：`DETAIL / FORMULA / GROUP` |
| amount | 本期净额 |
| drillable | 是否可下钻 |

## 校验字段（ReportCheckResultVO）
| 字段 | 含义 |
|---|---|
| code | 校验编码 |
| passed | 是否通过 |
| message | 校验说明 |
