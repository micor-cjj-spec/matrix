# 后端提示词：企业纳税表税种拆分与税负提示

请在 `matrix` 后端增强企业纳税表接口：

1. 修改 `BizfiFiAnalysisReportServiceImpl.enterpriseTax`。
2. 基于利润表营业收入、净利润生成不少于 5 条税种拆分行。
3. 税种至少包含：增值税、城市维护建设税、教育费附加、地方教育附加、企业所得税、印花税。
4. 计算税费预估合计和综合税负率。
5. 当前结果必须明确为分析预估口径，不替代正式纳税申报。
6. 保持接口路径 `GET /analysis-report/enterprise-tax` 兼容。
