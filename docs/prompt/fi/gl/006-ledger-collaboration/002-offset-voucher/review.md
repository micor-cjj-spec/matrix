# 对冲凭证评审提示词

## 目标
根据 `docs/bus/fi/gl/006-ledger-collaboration/002-offset-voucher/` 下业务文档、当前 prompt 文档，以及已确认实现，输出对冲凭证场景的一致性评审提示词。

## 输入上下文
请结合以下文档：
- `business.md`
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
- `docs/prompt/fi/gl/006-ledger-collaboration/002-offset-voucher/test.md`

## 输出要求
1. 检查 BUS 与 prompt 是否一致
2. 检查已配对、孤儿记录、原凭证金额、对冲金额、金额差异、warnings 口径是否一致
3. 检查查询、表格展示、跳转查看原凭证/对冲凭证是否一致
4. 输出“已对齐 / 部分对齐 / 缺失 / 待确认”的评审清单
5. 标记后续待补项
