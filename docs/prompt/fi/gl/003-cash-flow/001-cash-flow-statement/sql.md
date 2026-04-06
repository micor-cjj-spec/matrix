# 现金流量表SQL提示词

## 目标
根据 `docs/bus/fi/gl/003-cash-flow/001-cash-flow-statement/` 下业务文档，以及当前已确认实现，输出现金流量表场景的 SQL 与查询设计提示词。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `rules.md`
- `api.md`
- `docs/prompt/fi/gl/003-cash-flow/001-cash-flow-statement/dictionary.md`
- `docs/prompt/fi/gl/003-cash-flow/001-cash-flow-statement/backend.md`

同时结合当前已确认实现：
- 基于已过账总账分录、现金流项目主数据、报表模板和报表项目生成结果
- 需要按 orgId、period、currency、templateId、showZero 生成现金流量表视图

## 输出要求
1. 输出 rows、checks、warnings 所需查询口径
2. 输出 orgId、period、currency、templateId、showZero 过滤口径
3. 输出模板、项目、现金流项目映射与性能优化建议
4. 输出未知编码、现金划转、启发式分类相关数据来源建议
5. 对 BUS 未明确项显式标记“待确认”
