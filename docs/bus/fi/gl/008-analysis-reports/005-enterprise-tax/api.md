# 接口说明

## 查询接口
- `GET /analysis-report/enterprise-tax`

## 请求参数
| 参数 | 必填 | 说明 |
|---|---|---|
| orgId | 否 | 业务单元ID |
| period | 否 | 期间，格式 `yyyy-MM` |
| currency | 否 | 币种，默认 `CNY` |
| profitTemplateId | 否 | 利润表模板ID |

## 响应要点
- `revenueAmount`：营业收入。
- `netProfitAmount`：净利润。
- `totalTaxAmount`：税费预估合计。
- `taxBurdenRate`：综合税负率，小数口径。
- `rows`：税种拆分明细，当前不少于 6 行。
- `checks`：检查结果。
- `warnings`：风险提示。

## 前端关联
- 前端 API：`financialReportApi.fetchEnterpriseTax`
- 前端页面：`EnterpriseTaxView.vue`
