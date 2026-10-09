# 现金流量查询前端提示词

## 目标
结合 `docs/bus/fi/gl/003-cash-flow/002-cash-flow-query/` 下业务文档，以及当前前端已确认实现，输出现金流量查询场景的前端实现建议与补齐提示词。

## 输入上下文
请结合以下文档：
- `page.md`
- `fields.md`
- `flow.md`
- `api.md`
- `docs/prompt/fi/gl/003-cash-flow/002-cash-flow-query/dictionary.md`
- `docs/prompt/fi/gl/003-cash-flow/002-cash-flow-query/api.md`
- `docs/prompt/fi/gl/003-cash-flow/002-cash-flow-query/backend.md`

同时结合当前已确认实现：
- 页面：`CashFlowQueryView.vue`
- 查询区：业务单元、期间、现金流项目、活动分类、识别方式、科目编码关键字、关键字
- 汇总卡片、状态条、告警区、表格区
- 初始化时加载业务单元和现金流项目选项并自动查询
- 点击查看凭证跳转凭证页并带凭证号

## 输出要求
1. 输出查询区、汇总卡片、状态条、告警区、表格区交互建议
2. 说明初始化自动查询、查询/重置行为
3. 说明各种识别来源状态条、warnings、统计字段展示逻辑
4. 说明查看凭证跳转参数映射
5. 对 BUS 未明确项显式标记“待确认”
