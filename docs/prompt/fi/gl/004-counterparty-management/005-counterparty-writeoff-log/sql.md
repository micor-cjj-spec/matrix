# 往来核销日志SQL提示词

## 目标
根据 `docs/bus/fi/gl/004-counterparty-management/005-counterparty-writeoff-log/` 下业务文档，以及当前已确认实现，输出往来核销日志场景的 SQL 与查询设计提示词。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `rules.md`
- `api.md`
- `docs/prompt/fi/gl/004-counterparty-management/005-counterparty-writeoff-log/dictionary.md`
- `docs/prompt/fi/gl/004-counterparty-management/005-counterparty-writeoff-log/backend.md`

同时结合当前已确认实现：
- 读取 `BizfiFiArapWriteoffLog / BizfiFiArapWriteoffLink` 生成批次摘要与链接明细
- 需要按 docTypeRoot、counterparty、planCode、startDate、endDate 过滤

## 输出要求
1. 输出 `records`、`linkDetails`、`warnings` 与统计字段所需查询口径
2. 输出日志表、链接表、往来方关联口径
3. 输出批次数、链接数、核销金额、操作时间相关数据来源建议
4. 输出索引与性能优化建议
5. 对 BUS 未明确项显式标记“待确认”
