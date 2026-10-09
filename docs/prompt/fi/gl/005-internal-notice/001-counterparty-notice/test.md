# 往来通知单测试提示词

## 目标
根据 `docs/bus/fi/gl/005-internal-notice/001-counterparty-notice/` 下业务文档，以及当前前后端已确认实现，输出往来通知单场景的测试与验证提示词。

## 输入上下文
请结合以下文档：
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`
- `docs/prompt/fi/gl/005-internal-notice/001-counterparty-notice/dictionary.md`
- `docs/prompt/fi/gl/005-internal-notice/001-counterparty-notice/api.md`
- `docs/prompt/fi/gl/005-internal-notice/001-counterparty-notice/backend.md`
- `docs/prompt/fi/gl/005-internal-notice/001-counterparty-notice/frontend.md`
- `docs/prompt/fi/gl/005-internal-notice/001-counterparty-notice/sql.md`

## 输出要求
1. 覆盖查询场景
2. 覆盖通知状态、紧急程度、统计日期过滤场景
3. 覆盖生成通知单、成功提示区、warnings、表格展示场景
4. 覆盖跳转到对账单/勾稽页面、空结果、异常结果场景
5. 对 BUS 未明确项显式标记“待确认”
