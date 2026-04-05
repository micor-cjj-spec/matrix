# 凭证汇总表前端提示词

## 目标
结合 `docs/bus/fi/gl/001-voucher-processing/002-voucher-summary/` 下业务文档，以及当前前端已确认实现，输出凭证汇总表场景的前端实现建议与补齐提示词。

## 输入上下文
请结合以下文档：
- `page.md`
- `fields.md`
- `flow.md`
- `api.md`
- `docs/prompt/fi/gl/001-voucher-processing/002-voucher-summary/dictionary.md`
- `docs/prompt/fi/gl/001-voucher-processing/002-voucher-summary/api.md`
- `docs/prompt/fi/gl/001-voucher-processing/002-voucher-summary/backend.md`

同时结合当前已确认实现：
- 页面：`VoucherSummaryView.vue`
- 查询区：开始日期、结束日期、状态、摘要关键字
- 汇总卡片、warnings 告警区、日期粒度表格区
- 查看凭证跳转 `/ledger/voucher`

## 输出要求
1. 输出查询区、汇总卡片、告警区、表格区交互建议
2. 说明默认查询、本月区间、查询/重置行为
3. 说明 warnings 展示、空结果展示、加载态与异常态展示
4. 说明“查看凭证”跳转参数拼装逻辑
5. 对 BUS 未明确项显式标记“待确认”
