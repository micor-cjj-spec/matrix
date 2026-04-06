# 现金流量表后端提示词

## 目标
结合 `docs/bus/fi/gl/003-cash-flow/001-cash-flow-statement/` 下业务文档，以及当前后端已确认实现，输出现金流量表场景的后端实现建议与补齐提示词。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `api.md`
- `docs/prompt/fi/gl/003-cash-flow/001-cash-flow-statement/dictionary.md`
- `docs/prompt/fi/gl/003-cash-flow/001-cash-flow-statement/api.md`

同时结合当前已确认实现：
- 查询服务：`BizfiFiCashFlowServiceImpl.query`
- 基于已过账总账分录、现金流项目主数据、报表模板和报表项目生成结果
- 返回 `rows`、`checks`、`warnings`

## 输出要求
1. 说明当前后端查询能力与待补齐项
2. 输出经营/投资/筹资活动与净增加额的计算逻辑建议
3. 说明未知编码、现金划转、启发式分类等 `checks` / `warnings` 生成逻辑
4. 说明模板、币种、showZero 过滤与性能处理建议
5. 对 BUS 未明确项显式标记“待确认”
