# 科目余额对照数据字典提示词

## 目标
根据 `docs/bus/fi/gl/006-ledger-collaboration/004-subject-compare/` 下业务文档，输出科目余额对照场景的数据字典、统计口径、差异口径、告警口径与跳转口径，作为接口、前后端、测试的统一语义基础。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`

同时结合当前已确认实现：
- 后端服务：`BizfiFiLedgerCollaborationServiceImpl.subjectBalanceCompare`
- 数据来源：凭证分录汇总口径与总账分录汇总口径
- 前端页面：`SubjectCompareView.vue`
- 接口：`GET /ledger-collaboration/subject-balance-compare`

## 输出要求
1. 输出查询条件字段字典
2. 输出汇总卡片字段字典
3. 输出科目对照结果表字段字典
4. 输出科目数、差异科目数、凭证借贷合计、总账借贷合计、借贷差值、余额差额等统计口径
5. 输出 warnings 与跳转到科目余额表页面口径
6. 对 BUS 未明确项显式标记“待确认”
