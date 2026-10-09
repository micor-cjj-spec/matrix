# 科目余额表数据字典提示词

## 目标
根据 `docs/bus/fi/gl/002-ledger-inquiry/001-subject-balance/` 下业务文档，输出科目余额表场景的数据字典、统计口径、告警口径与跳转/筛选口径，作为接口、前后端、测试的统一语义基础。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`

同时结合当前已确认实现：
- 后端服务：`BizfiFiLedgerQueryServiceImpl.subjectBalance`
- 数据来源：已过账总账分录 `BizfiFiGlEntry`
- 前端页面：`SubjectBalanceView.vue`
- 接口：`GET /ledger/subject-balance`

## 输出要求
1. 输出查询条件字段字典
2. 输出汇总卡片字段字典
3. 输出余额表行字段字典
4. 输出期初、本期借贷、期末余额的统计口径
5. 输出 warnings 口径与待确认项
