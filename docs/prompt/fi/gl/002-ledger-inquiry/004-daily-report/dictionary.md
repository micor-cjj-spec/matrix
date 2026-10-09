# 日报表数据字典提示词

## 目标
根据 `docs/bus/fi/gl/002-ledger-inquiry/004-daily-report/` 下业务文档，输出日报表场景的数据字典、统计口径、告警口径与展示口径，作为接口、前后端、测试的统一语义基础。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`

同时结合当前已确认实现：
- 后端服务：`BizfiFiLedgerQueryServiceImpl.dailyReport`
- 数据来源：已过账总账分录 `BizfiFiGlEntry` 按日聚合
- 前端页面：`DailyReportView.vue`
- 接口：`GET /ledger/daily-report`

## 输出要求
1. 输出查询条件字段字典
2. 输出汇总卡片字段字典
3. 输出日报表行字段字典
4. 输出按日借方、贷方、凭证数、滚动余额等统计口径
5. 输出 warnings 口径与待确认项
