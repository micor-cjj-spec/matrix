# 账龄分析表评审提示词

## 目标
根据 `docs/bus/fi/gl/004-counterparty-management/006-aging-analysis/` 下业务文档、当前 prompt 文档，以及已确认实现，输出账龄分析表场景的一致性评审提示词。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`
- `docs/prompt/fi/gl/004-counterparty-management/006-aging-analysis/dictionary.md`
- `docs/prompt/fi/gl/004-counterparty-management/006-aging-analysis/api.md`
- `docs/prompt/fi/gl/004-counterparty-management/006-aging-analysis/backend.md`
- `docs/prompt/fi/gl/004-counterparty-management/006-aging-analysis/frontend.md`
- `docs/prompt/fi/gl/004-counterparty-management/006-aging-analysis/sql.md`
- `docs/prompt/fi/gl/004-counterparty-management/006-aging-analysis/test.md`

## 输出要求
1. 检查 BUS 与 prompt 是否一致
2. 检查未核销余额、账龄分桶、信用额度、风险状态、warnings 口径是否一致
3. 检查查询、表格展示、只读分析边界是否一致
4. 输出“已对齐 / 部分对齐 / 缺失 / 待确认”的评审清单
5. 标记后续待补项
