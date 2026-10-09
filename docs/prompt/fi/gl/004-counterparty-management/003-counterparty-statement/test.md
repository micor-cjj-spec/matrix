# 往来对账单测试提示词

## 目标
根据 `docs/bus/fi/gl/004-counterparty-management/003-counterparty-statement/` 下业务文档，以及当前前后端已确认实现，输出往来对账单场景的测试与验证提示词。

## 输入上下文
请结合以下文档：
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`
- `docs/prompt/fi/gl/004-counterparty-management/003-counterparty-statement/dictionary.md`
- `docs/prompt/fi/gl/004-counterparty-management/003-counterparty-statement/api.md`
- `docs/prompt/fi/gl/004-counterparty-management/003-counterparty-statement/backend.md`
- `docs/prompt/fi/gl/004-counterparty-management/003-counterparty-statement/frontend.md`
- `docs/prompt/fi/gl/004-counterparty-management/003-counterparty-statement/sql.md`

## 输出要求
1. 覆盖查询场景
2. 覆盖往来类型、往来方、统计日期、仅看未核销过滤场景
3. 覆盖汇总卡片、warnings、对账明细表、最近核销批次表展示场景
4. 覆盖关联凭证跳转、核销日志跳转、空结果、异常结果场景
5. 对 BUS 未明确项显式标记“待确认”
