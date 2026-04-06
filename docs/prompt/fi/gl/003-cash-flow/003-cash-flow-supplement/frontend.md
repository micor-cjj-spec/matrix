# 现金流量补充资料前端提示词

## 目标
结合 `docs/bus/fi/gl/003-cash-flow/003-cash-flow-supplement/` 下业务文档，以及当前前端已确认实现，输出现金流量补充资料场景的前端实现建议与补齐提示词。

## 输入上下文
请结合以下文档：
- `page.md`
- `fields.md`
- `flow.md`
- `api.md`
- `docs/prompt/fi/gl/003-cash-flow/003-cash-flow-supplement/dictionary.md`
- `docs/prompt/fi/gl/003-cash-flow/003-cash-flow-supplement/api.md`
- `docs/prompt/fi/gl/003-cash-flow/003-cash-flow-supplement/backend.md`

同时结合当前已确认实现：
- 页面：`CashFlowSupplementView.vue`
- 查询区：业务单元、期间
- 汇总卡片、状态条、检查项表、分类分布表、待补录/复核凭证表、告警区
- 初始化时加载业务单元并自动查询
- 点击查看凭证跳转凭证页并带凭证号

## 输出要求
1. 输出查询区、汇总卡片、状态条、检查项表、分类分布表、待补录/复核凭证表、告警区交互建议
2. 说明初始化自动查询、查询/重置行为
3. 说明各种状态条、warnings、统计字段展示逻辑
4. 说明查看凭证跳转参数映射
5. 对 BUS 未明确项显式标记“待确认”
