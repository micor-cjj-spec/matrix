# 结转清单前端提示词

## 目标
结合 `docs/bus/fi/gl/001-voucher-processing/003-carry-list/` 下业务文档，以及当前前端已确认实现，输出结转清单场景的前端实现建议与补齐提示词。

## 输入上下文
请结合以下文档：
- `page.md`
- `fields.md`
- `flow.md`
- `api.md`
- `docs/prompt/fi/gl/001-voucher-processing/003-carry-list/dictionary.md`
- `docs/prompt/fi/gl/001-voucher-processing/003-carry-list/api.md`
- `docs/prompt/fi/gl/001-voucher-processing/003-carry-list/backend.md`

同时结合当前已确认实现：
- 页面：`CarryForwardListView.vue`
- 查询区：业务单元、期间
- 元信息区、汇总卡片、检查清单表、相关凭证表
- 查看凭证跳转 `/ledger/voucher`

## 输出要求
1. 输出查询区、元信息区、汇总卡片、检查清单表、相关凭证表交互建议
2. 说明默认期间、查询/重置、业务单元加载行为
3. 说明 warnings 展示、空结果展示、异常态展示
4. 说明“查看凭证”跳转参数拼装逻辑
5. 对 BUS 未明确项显式标记“待确认”
