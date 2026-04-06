# 往来核销日志数据字典提示词

## 目标
根据 `docs/bus/fi/gl/004-counterparty-management/005-counterparty-writeoff-log/` 下业务文档，输出往来核销日志场景的数据字典、统计口径、审计口径、告警口径与查看明细口径，作为接口、前后端、测试的统一语义基础。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`

同时结合当前已确认实现：
- 后端服务：`BizfiFiArapManageServiceImpl.writeoffLog`
- 数据来源：`BizfiFiArapWriteoffLog / BizfiFiArapWriteoffLink`
- 前端页面：`CounterpartyWriteoffLogView.vue`
- 接口：`GET /arap-manage/writeoff-log`

## 输出要求
1. 输出查询条件字段字典
2. 输出汇总卡片字段字典
3. 输出批次表与明细表字段字典
4. 输出批次数、链接数、核销金额、明细查看状态等统计口径
5. 输出 warnings 与查看明细带回 planCode 口径
6. 对 BUS 未明确项显式标记“待确认”
