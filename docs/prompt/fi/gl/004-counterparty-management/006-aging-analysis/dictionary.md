# 账龄分析表数据字典提示词

## 目标
根据 `docs/bus/fi/gl/004-counterparty-management/006-aging-analysis/` 下业务文档，输出账龄分析表场景的数据字典、统计口径、账龄分桶口径、风险口径与告警口径，作为接口、前后端、测试的统一语义基础。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`

同时结合当前已确认实现：
- 后端服务：`BizfiFiArapManageServiceImpl.agingAnalysis`
- 数据来源：往来单据、核销链接、往来方信用额度主数据
- 前端页面：`CounterpartyAgingAnalysisView.vue`
- 接口：`GET /arap-manage/aging-analysis`

## 输出要求
1. 输出查询条件字段字典
2. 输出汇总卡片字段字典
3. 输出账龄分析结果表字段字典
4. 输出账龄分桶、信用额度、逾期阈值、风险状态等口径
5. 输出 warnings 与待确认项
