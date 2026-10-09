# 对冲凭证前端提示词

## 目标
结合 `docs/bus/fi/gl/006-ledger-collaboration/002-offset-voucher/` 下业务文档，以及当前前端已确认实现，输出对冲凭证场景的前端实现建议与补齐提示词。

## 输入上下文
请结合以下文档：
- `page.md`
- `fields.md`
- `flow.md`
- `api.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/002-offset-voucher/dictionary.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/002-offset-voucher/api.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/002-offset-voucher/backend.md`

同时结合当前已确认实现：
- 页面：`OffsetVoucherView.vue`
- 查询区：开始日期、结束日期、配对状态、关键字
- 汇总卡片、告警区、表格区
- 点击“查询”调用对冲凭证接口
- 行内可分别跳转查看原凭证和对冲凭证

## 输出要求
1. 输出查询区、汇总卡片、告警区、表格区交互建议
2. 说明查询行为与空结果、异常态展示
3. 说明 warnings、配对状态、金额差异展示逻辑
4. 说明跳转查看原凭证和对冲凭证的参数映射
5. 对 BUS 未明确项显式标记“待确认”
