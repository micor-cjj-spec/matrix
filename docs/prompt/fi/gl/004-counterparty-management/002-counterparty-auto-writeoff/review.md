# 往来自动核销评审提示词

## 目标
根据 `docs/bus/fi/gl/004-counterparty-management/002-counterparty-auto-writeoff/` 下业务文档、当前 prompt 文档，以及已确认实现，输出往来自动核销场景的一致性评审提示词。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`
- `docs/prompt/fi/gl/004-counterparty-management/002-counterparty-auto-writeoff/dictionary.md`
- `docs/prompt/fi/gl/004-counterparty-management/002-counterparty-auto-writeoff/api.md`
- `docs/prompt/fi/gl/004-counterparty-management/002-counterparty-auto-writeoff/backend.md`
- `docs/prompt/fi/gl/004-counterparty-management/002-counterparty-auto-writeoff/frontend.md`
- `docs/prompt/fi/gl/004-counterparty-management/002-counterparty-auto-writeoff/sql.md`
- `docs/prompt/fi/gl/004-counterparty-management/002-counterparty-auto-writeoff/test.md`

## 输出要求
1. 检查 BUS 与 prompt 是否一致
2. 检查方案号、日志号、核销链接数、核销总额、warnings 口径是否一致
3. 检查条件带入、自动预览、执行自动核销、成功提示区展示是否一致
4. 输出“已对齐 / 部分对齐 / 缺失 / 待确认”的评审清单
5. 标记后续待补项
