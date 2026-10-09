# 往来核销日志测试提示词

## 目标
根据 `docs/bus/fi/gl/004-counterparty-management/005-counterparty-writeoff-log/` 下业务文档，以及当前前后端已确认实现，输出往来核销日志场景的测试与验证提示词。

## 输入上下文
请结合以下文档：
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`
- `docs/prompt/fi/gl/004-counterparty-management/005-counterparty-writeoff-log/dictionary.md`
- `docs/prompt/fi/gl/004-counterparty-management/005-counterparty-writeoff-log/api.md`
- `docs/prompt/fi/gl/004-counterparty-management/005-counterparty-writeoff-log/backend.md`
- `docs/prompt/fi/gl/004-counterparty-management/005-counterparty-writeoff-log/frontend.md`
- `docs/prompt/fi/gl/004-counterparty-management/005-counterparty-writeoff-log/sql.md`

## 输出要求
1. 覆盖查询场景
2. 覆盖往来类型、往来方、方案号、开始日期、结束日期过滤场景
3. 覆盖汇总卡片、批次表、明细表、warnings 展示场景
4. 覆盖点击批次查看明细、空结果、异常结果场景
5. 对 BUS 未明确项显式标记“待确认”
