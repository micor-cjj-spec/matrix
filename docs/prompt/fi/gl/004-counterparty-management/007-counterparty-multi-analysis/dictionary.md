# 往来多维分析表数据字典提示词

## 目标
根据 `docs/bus/fi/gl/004-counterparty-management/007-counterparty-multi-analysis/` 下业务文档，输出往来多维分析表场景的数据字典、统计口径、分组口径、告警口径与展示口径，作为接口、前后端、测试的统一语义基础。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`

同时结合当前已确认实现：
- 后端服务：`BizfiFiArapManageServiceImpl.multiAnalysis`
- 数据来源：往来单据、已落库核销链接
- 前端页面：`CounterpartyMultiAnalysisView.vue`
- 接口：`GET /arap-manage/multi-analysis`

## 输出要求
1. 输出查询条件字段字典
2. 输出汇总卡片字段字典
3. 输出多维聚合结果表字段字典
4. 输出 groupDimension、原额、已核销、未核销等统计口径
5. 输出 warnings 与待确认项
