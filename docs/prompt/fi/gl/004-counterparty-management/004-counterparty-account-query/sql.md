# 往来账查询SQL提示词

## 目标
根据 `docs/bus/fi/gl/004-counterparty-management/004-counterparty-account-query/` 下业务文档，以及当前已确认实现，输出往来账查询场景的 SQL 与查询设计提示词。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `rules.md`
- `api.md`
- `docs/prompt/fi/gl/004-counterparty-management/004-counterparty-account-query/dictionary.md`
- `docs/prompt/fi/gl/004-counterparty-management/004-counterparty-account-query/backend.md`

同时结合当前已确认实现：
- 基于往来单据与已落库核销链接生成结果
- 需要按 docTypeRoot、counterparty、docType、status、openOnly、keyword、asOfDate 过滤

## 输出要求
1. 输出 `records`、`warnings` 与统计字段所需查询口径
2. 输出往来单据、核销链接关联口径
3. 输出原额、已核销、未核销、核销状态、账龄相关数据来源建议
4. 输出索引与性能优化建议
5. 对 BUS 未明确项显式标记“待确认”
