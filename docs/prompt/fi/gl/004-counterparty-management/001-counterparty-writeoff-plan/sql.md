# 往来核销方案SQL提示词

## 目标
根据 `docs/bus/fi/gl/004-counterparty-management/001-counterparty-writeoff-plan/` 下业务文档，以及当前已确认实现，输出往来核销方案场景的 SQL 与查询设计提示词。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `rules.md`
- `api.md`
- `docs/prompt/fi/gl/004-counterparty-management/001-counterparty-writeoff-plan/dictionary.md`
- `docs/prompt/fi/gl/004-counterparty-management/001-counterparty-writeoff-plan/backend.md`

同时结合当前已确认实现：
- 基于 AR/AP 单据与已落库核销链接生成预览方案
- 需要按 docTypeRoot、counterparty、asOfDate、auditedOnly 过滤

## 输出要求
1. 输出 `records`、`warnings`、统计字段所需查询口径
2. 输出源单据、结算单据、已落库核销链接关联口径
3. 输出剩余金额与建议核销金额计算相关数据来源建议
4. 输出索引与性能优化建议
5. 对 BUS 未明确项显式标记“待确认”
