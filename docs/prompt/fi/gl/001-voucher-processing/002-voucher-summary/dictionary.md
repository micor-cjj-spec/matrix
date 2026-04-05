# 凭证汇总表数据字典提示词

## 目标
根据 `docs/bus/fi/gl/001-voucher-processing/002-voucher-summary/` 下业务文档，输出凭证汇总表场景统一的数据字典、统计口径、状态口径、告警口径与跳转参数口径，作为接口、前后端、SQL、测试的统一语义基础。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`

同时结合当前已确认实现：
- 后端服务：`BizfiFiVoucherAnalysisServiceImpl.summary`
- 前端页面：`VoucherSummaryView.vue`
- 汇总接口：`GET /voucher/summary`
- 反查跳转：`/ledger/voucher`

## 输出要求
1. 输出查询条件字段字典
2. 输出汇总卡片字段字典
3. 输出表格行字段字典
4. 输出状态统计口径、金额统计口径、异常/告警口径
5. 输出跳转参数口径与待确认项
