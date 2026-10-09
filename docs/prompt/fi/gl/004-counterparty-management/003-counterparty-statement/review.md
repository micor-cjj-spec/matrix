# 往来对账单评审提示词

## 目标
根据 `docs/bus/fi/gl/004-counterparty-management/003-counterparty-statement/` 下业务文档、当前 prompt 文档，以及已确认实现，输出往来对账单场景的一致性评审提示词。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`
- `docs/prompt/fi/gl/004-counterparty-management/003-counterparty-statement/dictionary.md`
- `docs/prompt/fi/gl/004-counterparty-management/003-counterparty-statement/api.md`
- `docs/prompt/fi/gl/004-counterparty-management/003-counterparty-statement/backend.md`
- `docs/prompt/fi/gl/004-counterparty-management/003-counterparty-statement/frontend.md`
- `docs/prompt/fi/gl/004-counterparty-management/003-counterparty-statement/sql.md`
- `docs/prompt/fi/gl/004-counterparty-management/003-counterparty-statement/test.md`

## 输出要求
1. 检查 BUS 与 prompt 是否一致
2. 检查原额、已核销金额、未核销金额、核销率、最近批次口径是否一致
3. 检查查询、表格展示、关联凭证跳转、核销日志跳转是否一致
4. 输出“已对齐 / 部分对齐 / 缺失 / 待确认”的评审清单
5. 标记后续待补项
