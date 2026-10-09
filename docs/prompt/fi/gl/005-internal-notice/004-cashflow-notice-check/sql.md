# 现金流通知单勾稽SQL提示词

## 目标
根据 `docs/bus/fi/gl/005-internal-notice/004-cashflow-notice-check/` 下业务文档，以及当前已确认实现，输出现金流通知单勾稽场景的 SQL 与查询设计提示词。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `rules.md`
- `api.md`
- `docs/prompt/fi/gl/005-internal-notice/004-cashflow-notice-check/dictionary.md`
- `docs/prompt/fi/gl/005-internal-notice/004-cashflow-notice-check/backend.md`

同时结合当前已确认实现：
- 基于内部通知单表与最新现金流候选结果生成勾稽结果
- 需要按 orgId、period、status、currency 过滤

## 输出要求
1. 输出 `rows`、`warnings` 与统计字段所需查询口径
2. 输出通知单表、现金流候选结果、勾稽状态关联口径
3. 输出快照金额、当前金额、仍需处理/已自然解除相关数据来源建议
4. 输出索引与性能优化建议
5. 对 BUS 未明确项显式标记“待确认”
