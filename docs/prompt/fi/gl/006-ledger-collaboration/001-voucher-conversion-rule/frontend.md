# 凭证折算规则前端提示词

## 目标
结合 `docs/bus/fi/gl/006-ledger-collaboration/001-voucher-conversion-rule/` 下业务文档，以及当前前端已确认实现，输出凭证折算规则场景的前端实现建议与补齐提示词。

## 输入上下文
请结合以下文档：
- `page.md`
- `fields.md`
- `flow.md`
- `api.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/001-voucher-conversion-rule/dictionary.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/001-voucher-conversion-rule/api.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/001-voucher-conversion-rule/backend.md`

同时结合当前已确认实现：
- 页面：`VoucherRuleView.vue`
- 查询区：往来类型、关键字
- 汇总卡片、告警区、表格区
- 点击“查询”调用凭证折算规则接口
- 行内可跳转到应收或应付业务单据页面

## 输出要求
1. 输出查询区、汇总卡片、告警区、表格区交互建议
2. 说明查询行为与空结果、异常态展示
3. 说明 warnings、覆盖率、待生成量、最近单据日期展示逻辑
4. 说明跳转到应收/应付业务单据页面的参数映射
5. 对 BUS 未明确项显式标记“待确认”
