# 科目余额对照评审提示词

## 目标
根据 `docs/bus/fi/gl/006-ledger-collaboration/004-subject-compare/` 下业务文档、当前 prompt 文档，以及已确认实现，输出科目余额对照场景的一致性评审提示词。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/004-subject-compare/dictionary.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/004-subject-compare/api.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/004-subject-compare/backend.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/004-subject-compare/frontend.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/004-subject-compare/sql.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/004-subject-compare/test.md`

## 输出要求
1. 检查 BUS 与 prompt 是否一致
2. 检查科目数、差异科目数、凭证借贷合计、总账借贷合计、借贷差值、余额差额、warnings 口径是否一致
3. 检查初始化自动查询、查询、表格展示、跳转到科目余额表页面是否一致
4. 输出“已对齐 / 部分对齐 / 缺失 / 待确认”的评审清单
5. 标记后续待补项
