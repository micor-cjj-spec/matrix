# 企业纳税表税种拆分与税负提示分析记录

## 现状
- 后端接口：`GET /analysis-report/enterprise-tax`
- 后端实现：`BizfiFiAnalysisReportServiceImpl.enterpriseTax`
- 前端页面：`src/views/login/ledger/report/EnterpriseTaxView.vue`
- 当前税种行：增值税预估、附加税费预估、企业所得税预估，共 3 行。

## 需求判断
本功能属于总账分析报表向税务域延伸的轻量闭环能力，不虚构正式税务申报流程，仅增强已落地接口的分析口径。

## 设计取舍
- 继续复用利润表结果作为计算来源。
- 将附加税费拆分为城市维护建设税、教育费附加、地方教育附加。
- 新增印花税预估行，使报表行数达到 6 行。
- 新增综合税负率，按税费预估合计 / 营业收入计算。
