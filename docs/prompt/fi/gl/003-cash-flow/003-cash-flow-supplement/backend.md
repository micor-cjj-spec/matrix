# 现金流量补充资料后端提示词

## 目标
结合 `docs/bus/fi/gl/003-cash-flow/003-cash-flow-supplement/` 下业务文档，以及当前后端已确认实现，输出现金流量补充资料场景的后端实现建议与补齐提示词。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `api.md`
- `docs/prompt/fi/gl/003-cash-flow/003-cash-flow-supplement/dictionary.md`
- `docs/prompt/fi/gl/003-cash-flow/003-cash-flow-supplement/api.md`

同时结合当前已确认实现：
- 查询服务：`BizfiFiCashFlowServiceImpl.supplement`
- 返回 `tasks`、`categories`、`pendingVouchers`、`warnings` 与统计字段

## 输出要求
1. 说明当前后端补充资料能力与待补齐项
2. 输出未知编码、多编码、规则推断、现金划转等任务生成逻辑建议
3. 说明 orgId、period、currency 过滤与分类分布统计逻辑
4. 说明性能、空结果与异常结果处理建议
5. 对 BUS 未明确项显式标记“待确认”
