# 凭证协同检查前端提示词

## 目标
结合 `docs/bus/fi/gl/006-ledger-collaboration/003-voucher-collaboration-check/` 下业务文档，以及当前前端已确认实现，输出凭证协同检查场景的前端实现建议与补齐提示词。

## 输入上下文
请结合以下文档：
- `page.md`
- `fields.md`
- `flow.md`
- `api.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/003-voucher-collaboration-check/dictionary.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/003-voucher-collaboration-check/api.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/003-voucher-collaboration-check/backend.md`

同时结合当前已确认实现：
- 页面：`VoucherCollaborationCheckView.vue`
- 查询区：开始日期、结束日期、凭证状态、问题类型、问题等级、仅看异常
- 汇总卡片、告警区、表格区
- 点击“查询”调用凭证协同检查接口
- 行内可跳转查看凭证

## 输出要求
1. 输出查询区、汇总卡片、告警区、表格区交互建议
2. 说明查询行为与空结果、异常态展示
3. 说明 warnings、问题类型、问题等级、仅看异常展示逻辑
4. 说明跳转查看凭证的参数映射
5. 对 BUS 未明确项显式标记“待确认”
