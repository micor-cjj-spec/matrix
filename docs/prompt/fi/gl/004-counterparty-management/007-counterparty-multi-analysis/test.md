# 往来多维分析表测试提示词

## 目标
根据 `docs/bus/fi/gl/004-counterparty-management/007-counterparty-multi-analysis/` 下业务文档，以及当前前后端已确认实现，输出往来多维分析表场景的测试与验证提示词。

## 输入上下文
请结合以下文档：
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`
- `docs/prompt/fi/gl/004-counterparty-management/007-counterparty-multi-analysis/dictionary.md`
- `docs/prompt/fi/gl/004-counterparty-management/007-counterparty-multi-analysis/api.md`
- `docs/prompt/fi/gl/004-counterparty-management/007-counterparty-multi-analysis/backend.md`
- `docs/prompt/fi/gl/004-counterparty-management/007-counterparty-multi-analysis/frontend.md`
- `docs/prompt/fi/gl/004-counterparty-management/007-counterparty-multi-analysis/sql.md`

## 输出要求
1. 覆盖查询场景
2. 覆盖往来类型、汇总维度、统计日期过滤场景
3. 覆盖汇总卡片、warnings、表格展示场景
4. 覆盖不同 groupDimension、空结果、异常结果场景
5. 对 BUS 未明确项显式标记“待确认”
