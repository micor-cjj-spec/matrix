# 现金流通知单勾稽测试提示词

## 目标
根据 `docs/bus/fi/gl/005-internal-notice/004-cashflow-notice-check/` 下业务文档，以及当前前后端已确认实现，输出现金流通知单勾稽场景的测试与验证提示词。

## 输入上下文
请结合以下文档：
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

## 输出要求
1. 覆盖初始化自动查询与查询场景
2. 覆盖通知状态、期间过滤场景
3. 覆盖汇总卡片、warnings、表格展示场景
4. 覆盖跳转到凭证页、空结果、异常结果场景
5. 对 BUS 未明确项显式标记“待确认”
