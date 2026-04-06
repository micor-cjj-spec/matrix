# 往来核销方案评审提示词

## 目标
根据 `docs/bus/fi/gl/004-counterparty-management/001-counterparty-writeoff-plan/` 下业务文档、当前 prompt 文档，以及已确认实现，输出往来核销方案场景的一致性评审提示词。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`
- `docs/prompt/fi/gl/004-counterparty-management/001-counterparty-writeoff-plan/dictionary.md`
- `docs/prompt/fi/gl/004-counterparty-management/001-counterparty-writeoff-plan/api.md`
- `docs/prompt/fi/gl/004-counterparty-management/001-counterparty-writeoff-plan/backend.md`
- `docs/prompt/fi/gl/004-counterparty-management/001-counterparty-writeoff-plan/frontend.md`
- `docs/prompt/fi/gl/004-counterparty-management/001-counterparty-writeoff-plan/sql.md`
- `docs/prompt/fi/gl/004-counterparty-management/001-counterparty-writeoff-plan/test.md`

## 输出要求
1. 检查 BUS 与 prompt 是否一致
2. 检查建议核销金额、剩余金额、warnings 口径是否一致
3. 检查查询方案、表格展示、“去自动核销”跳转是否一致
4. 输出“已对齐 / 部分对齐 / 缺失 / 待确认”的评审清单
5. 标记后续待补项
