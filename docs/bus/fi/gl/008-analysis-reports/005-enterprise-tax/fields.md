# 字段设计

## 查询参数
| 参数 | 含义 | 说明 |
|---|---|---|
| orgId | 业务单元 | 可空 |
| period | 期间 | `yyyy-MM`，为空时默认当前期间 |
| currency | 币种 | 默认 `CNY` |
| profitTemplateId | 利润表模板ID | 可空 |

## 返回汇总字段
| 字段 | 含义 |
|---|---|
| orgId | 业务单元ID |
| period | 期间 |
| currency | 币种 |
| revenueAmount | 营业收入 |
| netProfitAmount | 净利润 |
| totalTaxAmount | 税费预估合计 |
| taxBurdenRate | 综合税负率 |
| rows | 税种拆分行 |
| checks | 检查结果 |
| warnings | 风险提示 |

## 税种行字段
| 字段 | 含义 |
|---|---|
| taxCode | 税种编码 |
| taxName | 税种名称 |
| taxBase | 计税基础 |
| taxRate | 税率 |
| taxAmount | 税额 |
| note | 说明 |
