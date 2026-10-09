# 现金流量补充资料评审提示词

## 目标
根据 `docs/bus/fi/gl/003-cash-flow/003-cash-flow-supplement/` 下业务文档、当前 prompt 文档，以及已确认实现，输出现金流量补充资料场景的一致性评审提示词。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`
- `docs/prompt/fi/gl/003-cash-flow/003-cash-flow-supplement/dictionary.md`
- `docs/prompt/fi/gl/003-cash-flow/003-cash-flow-supplement/api.md`
- `docs/prompt/fi/gl/003-cash-flow/003-cash-flow-supplement/backend.md`
- `docs/prompt/fi/gl/003-cash-flow/003-cash-flow-supplement/frontend.md`
- `docs/prompt/fi/gl/003-cash-flow/003-cash-flow-supplement/sql.md`
- `docs/prompt/fi/gl/003-cash-flow/003-cash-flow-supplement/test.md`

## 输出要求
1. 检查 BUS 与 prompt 是否一致
2. 检查任务清单、分类分布、待补录/复核凭证、warnings 口径是否一致
3. 检查自动查询、状态条、查看凭证跳转是否一致
4. 输出“已对齐 / 部分对齐 / 缺失 / 待确认”的评审清单
5. 标记后续待补项
