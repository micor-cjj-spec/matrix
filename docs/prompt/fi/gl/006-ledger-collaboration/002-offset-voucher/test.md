# 对冲凭证测试提示词

## 目标
根据 `docs/bus/fi/gl/006-ledger-collaboration/002-offset-voucher/` 下业务文档，以及当前前后端已确认实现，输出对冲凭证场景的测试与验证提示词。

## 输入上下文
请结合以下文档：
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/002-offset-voucher/dictionary.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/002-offset-voucher/api.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/002-offset-voucher/backend.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/002-offset-voucher/frontend.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/002-offset-voucher/sql.md`

## 输出要求
1. 覆盖查询场景
2. 覆盖开始日期、结束日期、配对状态、关键字过滤场景
3. 覆盖汇总卡片、warnings、表格展示场景
4. 覆盖跳转查看原凭证/对冲凭证、空结果、异常结果场景
5. 对 BUS 未明确项显式标记“待确认”
