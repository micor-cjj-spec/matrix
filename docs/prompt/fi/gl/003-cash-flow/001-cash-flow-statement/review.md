# 现金流量表评审提示词

## 目标
根据 `docs/bus/fi/gl/003-cash-flow/001-cash-flow-statement/` 下业务文档、当前 prompt 文档，以及已确认实现，输出现金流量表场景的一致性评审提示词。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`
- `docs/prompt/fi/gl/003-cash-flow/001-cash-flow-statement/dictionary.md`
- `docs/prompt/fi/gl/003-cash-flow/001-cash-flow-statement/api.md`
- `docs/prompt/fi/gl/003-cash-flow/001-cash-flow-statement/backend.md`
- `docs/prompt/fi/gl/003-cash-flow/001-cash-flow-statement/frontend.md`
- `docs/prompt/fi/gl/003-cash-flow/001-cash-flow-statement/sql.md`
- `docs/prompt/fi/gl/003-cash-flow/001-cash-flow-statement/test.md`

## 输出要求
1. 检查 BUS 与 prompt 是否一致
2. 检查经营/投资/筹资活动、净增加额、checks、warnings 口径是否一致
3. 检查自动查询、模板信息、跳转按钮、表格展示是否一致
4. 输出“已对齐 / 部分对齐 / 缺失 / 待确认”的评审清单
5. 标记后续待补项
