# 明细分类账前端提示词

## 目标
结合 `docs/bus/fi/gl/002-ledger-inquiry/003-detail-ledger/` 下业务文档，以及当前前端已确认实现，输出明细分类账场景的前端实现建议与补齐提示词。

## 输入上下文
请结合以下文档：
- `page.md`
- `fields.md`
- `flow.md`
- `api.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/003-detail-ledger/dictionary.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/003-detail-ledger/api.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/003-detail-ledger/backend.md`

同时结合当前已确认实现：
- 页面：`DetailLedgerView.vue`
- 查询区：开始日期、结束日期、科目编码
- 汇总卡片、warnings 告警区、表格区
- 初始化时加载科目选项并自动查询

## 输出要求
1. 输出查询区、汇总卡片、告警区、表格区交互建议
2. 说明初始化自动查询、查询/重置行为
3. 说明科目选项加载、空结果展示、异常态展示
4. 说明滚动余额、余额方向等字段展示逻辑
5. 对 BUS 未明确项显式标记“待确认”
