# 往来核销日志评审提示词

## 目标
根据 `docs/bus/fi/gl/004-counterparty-management/005-counterparty-writeoff-log/` 下业务文档、当前 prompt 文档，以及已确认实现，输出往来核销日志场景的一致性评审提示词。

## 输入上下文
请结合以下文档：
- `business.md`
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
- `docs/prompt/fi/gl/004-counterparty-management/005-counterparty-writeoff-log/test.md`

## 输出要求
1. 检查 BUS 与 prompt 是否一致
2. 检查批次数、链接数、核销金额、操作时间、warnings 口径是否一致
3. 检查查询、批次表、明细表、查看明细联动是否一致
4. 输出“已对齐 / 部分对齐 / 缺失 / 待确认”的评审清单
5. 标记后续待补项
