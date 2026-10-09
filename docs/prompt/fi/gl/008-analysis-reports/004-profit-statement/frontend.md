# 利润表前端提示词

## 目标
结合 `docs/bus/fi/gl/008-analysis-reports/004-profit-statement/` 下业务文档，以及当前前端已确认实现，输出利润表场景的前端实现建议与补齐提示词。

## 输入上下文
请结合以下文档：
- `page.md`
- `fields.md`
- `flow.md`
- `api.md`
- `docs/prompt/fi/gl/008-analysis-reports/004-profit-statement/dictionary.md`
- `docs/prompt/fi/gl/008-analysis-reports/004-profit-statement/api.md`
- `docs/prompt/fi/gl/008-analysis-reports/004-profit-statement/backend.md`

同时结合当前已确认实现：
- 页面：`ProfitStatementView.vue`
- 查询区：业务单元、开始期间、结束期间、币种、模板、显示零值行
- 告警区、校验区、主表区
- 初始化时加载业务单元并自动查询
- 点击“查询”刷新利润表
- 当前页面未接入项目下钻弹窗

## 输出要求
1. 输出查询区、告警区、校验区、主表区交互建议
2. 说明初始化自动查询、查询、showZero 展示行为
3. 说明 warnings、checks、本期金额、本年累计金额展示逻辑
4. 明确标注“前端已接入，后端内部实现文件本轮未定位”
5. 对 BUS 未明确项显式标记“待确认”
