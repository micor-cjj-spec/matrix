# 凭证折算规则评审提示词

## 目标
根据 `docs/bus/fi/gl/006-ledger-collaboration/001-voucher-conversion-rule/` 下业务文档、当前 prompt 文档，以及已确认实现，输出凭证折算规则场景的一致性评审提示词。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/001-voucher-conversion-rule/dictionary.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/001-voucher-conversion-rule/api.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/001-voucher-conversion-rule/backend.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/001-voucher-conversion-rule/frontend.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/001-voucher-conversion-rule/sql.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/001-voucher-conversion-rule/test.md`

## 输出要求
1. 检查 BUS 与 prompt 是否一致
2. 检查规则数、已审核单据、已生成凭证、待生成、覆盖率、最近单据日期、warnings 口径是否一致
3. 检查查询、表格展示、跳转到应收/应付业务单据页面是否一致
4. 输出“已对齐 / 部分对齐 / 缺失 / 待确认”的评审清单
5. 标记后续待补项
