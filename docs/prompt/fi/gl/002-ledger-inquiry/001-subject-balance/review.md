# 科目余额表评审提示词

## 目标
根据 `docs/bus/fi/gl/002-ledger-inquiry/001-subject-balance/` 下业务文档、当前 prompt 文档，以及已确认实现，输出科目余额表场景的一致性评审提示词。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/001-subject-balance/dictionary.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/001-subject-balance/api.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/001-subject-balance/backend.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/001-subject-balance/frontend.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/001-subject-balance/sql.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/001-subject-balance/test.md`

## 输出要求
1. 检查 BUS 与 prompt 是否一致
2. 检查期初、本期借贷、期末余额口径是否一致
3. 检查 query 回填、warnings、summary、表格展示是否一致
4. 输出“已对齐 / 部分对齐 / 缺失 / 待确认”的评审清单
5. 标记后续待补项
