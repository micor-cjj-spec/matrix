# 企业纳税表税种拆分交付说明

## 本轮交付
- 企业纳税表从 3 条税费预估行扩展为 6 条税种拆分行。
- 新增综合税负率返回和前端展示。
- 更新企业纳税表业务文档、字段、流程、规则、页面、接口说明。
- 补充后端、前端、测试、评审提示词。

## 交付范围
- 后端：`GET /analysis-report/enterprise-tax`
- 前端：`/ledger/enterprise-tax`
- 文档：`docs/bus/fi/gl/008-analysis-reports/005-enterprise-tax`

## 验收
- 前端构建通过。
- 部署后线上查询正常。
- 企业纳税表明细不少于 5 行。
