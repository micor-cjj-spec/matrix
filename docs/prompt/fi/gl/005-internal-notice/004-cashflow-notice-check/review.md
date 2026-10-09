# 现金流通知单勾稽评审提示词

## 目标
根据 `docs/bus/fi/gl/005-internal-notice/004-cashflow-notice-check/` 下业务文档、当前 prompt 文档，以及已确认实现，输出现金流通知单勾稽场景的一致性评审提示词。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`
- `docs/prompt/fi/gl/005-internal-notice/004-cashflow-notice-check/dictionary.md`
- `docs/prompt/fi/gl/005-internal-notice/004-cashflow-notice-check/api.md`
- `docs/prompt/fi/gl/005-internal-notice/004-cashflow-notice-check/backend.md`
- `docs/prompt/fi/gl/005-internal-notice/004-cashflow-notice-check/frontend.md`
- `docs/prompt/fi/gl/005-internal-notice/004-cashflow-notice-check/sql.md`
- `docs/prompt/fi/gl/005-internal-notice/004-cashflow-notice-check/test.md`

## 输出要求
1. 检查 BUS 与 prompt 是否一致
2. 检查仍需处理、已自然解除、快照金额、当前金额、warnings 口径是否一致
3. 检查初始化自动查询、查询、表格展示、跳转到凭证页是否一致
4. 输出“已对齐 / 部分对齐 / 缺失 / 待确认”的评审清单
5. 标记后续待补项
