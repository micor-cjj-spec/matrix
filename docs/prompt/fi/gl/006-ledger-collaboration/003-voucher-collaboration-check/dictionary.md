# 凭证协同检查数据字典提示词

## 目标
根据 `docs/bus/fi/gl/006-ledger-collaboration/003-voucher-collaboration-check/` 下业务文档，输出凭证协同检查场景的数据字典、统计口径、问题类型口径、告警口径与跳转口径，作为接口、前后端、测试的统一语义基础。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`

同时结合当前已确认实现：
- 后端服务：`BizfiFiLedgerCollaborationServiceImpl.voucherChecks`
- 数据来源：凭证、凭证分录、总账分录联合检查
- 前端页面：`VoucherCollaborationCheckView.vue`
- 接口：`GET /ledger-collaboration/voucher-check`

## 输出要求
1. 输出查询条件字段字典
2. 输出汇总卡片字段字典
3. 输出协同检查结果表字段字典
4. 输出扫描凭证数、异常条数、高风险问题、健康凭证、问题类型、问题等级等统计口径
5. 输出 warnings 与跳转查看凭证口径
6. 对 BUS 未明确项显式标记“待确认”
