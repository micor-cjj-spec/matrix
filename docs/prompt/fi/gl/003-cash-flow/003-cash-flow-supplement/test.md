# 现金流量补充资料测试提示词

## 目标
根据 `docs/bus/fi/gl/003-cash-flow/003-cash-flow-supplement/` 下业务文档，以及当前前后端已确认实现，输出现金流量补充资料场景的测试与验证提示词。

## 输入上下文
请结合以下文档：
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

## 输出要求
1. 覆盖初始化自动查询、查询、重置场景
2. 覆盖业务单元、期间过滤场景
3. 覆盖状态条、检查项表、分类分布表、待补录/复核凭证表、warnings 展示场景
4. 覆盖查看凭证跳转、空结果、异常结果场景
5. 对 BUS 未明确项显式标记“待确认”
