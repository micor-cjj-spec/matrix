# 往来核销日志前端提示词

## 目标
结合 `docs/bus/fi/gl/004-counterparty-management/005-counterparty-writeoff-log/` 下业务文档，以及当前前端已确认实现，输出往来核销日志场景的前端实现建议与补齐提示词。

## 输入上下文
请结合以下文档：
- `page.md`
- `fields.md`
- `flow.md`
- `api.md`
- `docs/prompt/fi/gl/004-counterparty-management/005-counterparty-writeoff-log/dictionary.md`
- `docs/prompt/fi/gl/004-counterparty-management/005-counterparty-writeoff-log/api.md`
- `docs/prompt/fi/gl/004-counterparty-management/005-counterparty-writeoff-log/backend.md`

同时结合当前已确认实现：
- 页面：`CounterpartyWriteoffLogView.vue`
- 查询区：往来类型、往来方、方案号、开始日期、结束日期
- 汇总卡片、批次表、明细表、告警区
- 点击批次行“查看明细”后，把该批次 `planCode` 带回查询并刷新明细表

## 输出要求
1. 输出查询区、汇总卡片、批次表、明细表、告警区交互建议
2. 说明查询行为、查看明细行为、空结果与异常态展示
3. 说明 warnings、统计字段、明细查看状态展示逻辑
4. 说明 `planCode` 带回与批次/明细联动逻辑
5. 对 BUS 未明确项显式标记“待确认”
