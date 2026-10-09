# 往来账查询测试提示词

## 目标
根据 `docs/bus/fi/gl/004-counterparty-management/004-counterparty-account-query/` 下业务文档，以及当前前后端已确认实现，输出往来账查询场景的测试与验证提示词。

## 输入上下文
请结合以下文档：
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`
- `docs/prompt/fi/gl/004-counterparty-management/004-counterparty-account-query/dictionary.md`
- `docs/prompt/fi/gl/004-counterparty-management/004-counterparty-account-query/api.md`
- `docs/prompt/fi/gl/004-counterparty-management/004-counterparty-account-query/backend.md`
- `docs/prompt/fi/gl/004-counterparty-management/004-counterparty-account-query/frontend.md`
- `docs/prompt/fi/gl/004-counterparty-management/004-counterparty-account-query/sql.md`

## 输出要求
1. 覆盖查询场景
2. 覆盖往来类型、单据类型、单据状态、往来方、关键字、统计日期、仅看未核销过滤场景
3. 覆盖汇总卡片、warnings、表格展示场景
4. 覆盖空结果、异常结果、关联凭证查看场景
5. 对 BUS 未明确项显式标记“待确认”
