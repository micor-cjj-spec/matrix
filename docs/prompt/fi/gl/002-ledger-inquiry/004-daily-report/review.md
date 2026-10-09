# 日报表评审提示词

## 目标
根据 `docs/bus/fi/gl/002-ledger-inquiry/004-daily-report/` 下业务文档、当前 prompt 文档，以及已确认实现，输出日报表场景的一致性评审提示词。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/004-daily-report/dictionary.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/004-daily-report/api.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/004-daily-report/backend.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/004-daily-report/frontend.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/004-daily-report/sql.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/004-daily-report/test.md`

## 输出要求
1. 检查 BUS 与 prompt 是否一致
2. 检查按日借方、贷方、凭证数、滚动余额口径是否一致
3. 检查自动查询、summary、warnings、表格展示是否一致
4. 输出“已对齐 / 部分对齐 / 缺失 / 待确认”的评审清单
5. 标记后续待补项
