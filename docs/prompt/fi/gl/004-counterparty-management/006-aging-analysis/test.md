# 账龄分析表测试提示词

## 目标
根据 `docs/bus/fi/gl/004-counterparty-management/006-aging-analysis/` 下业务文档，以及当前前后端已确认实现，输出账龄分析表场景的测试与验证提示词。

## 输入上下文
请结合以下文档：
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

## 输出要求
1. 覆盖查询场景
2. 覆盖往来类型、统计日期过滤场景
3. 覆盖汇总卡片、warnings、表格展示场景
4. 覆盖账龄分桶、风险状态、空结果、异常结果场景
5. 对 BUS 未明确项显式标记“待确认”
