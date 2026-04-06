# 往来自动核销测试提示词

## 目标
根据 `docs/bus/fi/gl/004-counterparty-management/002-counterparty-auto-writeoff/` 下业务文档，以及当前前后端已确认实现，输出往来自动核销场景的测试与验证提示词。

## 输入上下文
请结合以下文档：
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`
- `docs/prompt/fi/gl/004-counterparty-management/002-counterparty-auto-writeoff/dictionary.md`
- `docs/prompt/fi/gl/004-counterparty-management/002-counterparty-auto-writeoff/api.md`
- `docs/prompt/fi/gl/004-counterparty-management/002-counterparty-auto-writeoff/backend.md`
- `docs/prompt/fi/gl/004-counterparty-management/002-counterparty-auto-writeoff/frontend.md`
- `docs/prompt/fi/gl/004-counterparty-management/002-counterparty-auto-writeoff/sql.md`

## 输出要求
1. 覆盖方案页带入条件、自动预览、预览方案场景
2. 覆盖执行自动核销场景
3. 覆盖统计字段、成功提示区、warnings、明细展示场景
4. 覆盖重复执行防护、空结果、异常结果场景
5. 对 BUS 未明确项显式标记“待确认”
