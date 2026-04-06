# 凭证协同检查评审提示词

## 目标
根据 `docs/bus/fi/gl/006-ledger-collaboration/003-voucher-collaboration-check/` 下业务文档、当前 prompt 文档，以及已确认实现，输出凭证协同检查场景的一致性评审提示词。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/003-voucher-collaboration-check/dictionary.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/003-voucher-collaboration-check/api.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/003-voucher-collaboration-check/backend.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/003-voucher-collaboration-check/frontend.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/003-voucher-collaboration-check/sql.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/003-voucher-collaboration-check/test.md`

## 输出要求
1. 检查 BUS 与 prompt 是否一致
2. 检查扫描凭证数、异常条数、高风险问题、健康凭证、问题类型、warnings 口径是否一致
3. 检查查询、表格展示、跳转查看凭证是否一致
4. 输出“已对齐 / 部分对齐 / 缺失 / 待确认”的评审清单
5. 标记后续待补项
