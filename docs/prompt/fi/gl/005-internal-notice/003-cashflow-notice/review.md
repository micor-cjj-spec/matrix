# 现金流通知单评审提示词

## 目标
根据 `docs/bus/fi/gl/005-internal-notice/003-cashflow-notice/` 下业务文档、当前 prompt 文档，以及已确认实现，输出现金流通知单场景的一致性评审提示词。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`
- `docs/prompt/fi/gl/005-internal-notice/003-cashflow-notice/dictionary.md`
- `docs/prompt/fi/gl/005-internal-notice/003-cashflow-notice/api.md`
- `docs/prompt/fi/gl/005-internal-notice/003-cashflow-notice/backend.md`
- `docs/prompt/fi/gl/005-internal-notice/003-cashflow-notice/frontend.md`
- `docs/prompt/fi/gl/005-internal-notice/003-cashflow-notice/sql.md`
- `docs/prompt/fi/gl/005-internal-notice/003-cashflow-notice/test.md`

## 输出要求
1. 检查 BUS 与 prompt 是否一致
2. 检查通知状态、紧急程度、问题来源、问题金额、生成结果统计口径是否一致
3. 检查初始化自动查询、查询、生成、成功提示区、跳转到凭证页/勾稽页面是否一致
4. 输出“已对齐 / 部分对齐 / 缺失 / 待确认”的评审清单
5. 标记后续待补项
