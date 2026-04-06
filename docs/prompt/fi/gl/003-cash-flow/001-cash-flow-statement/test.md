# 现金流量表测试提示词

## 目标
根据 `docs/bus/fi/gl/003-cash-flow/001-cash-flow-statement/` 下业务文档，以及当前前后端已确认实现，输出现金流量表场景的测试与验证提示词。

## 输入上下文
请结合以下文档：
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

## 输出要求
1. 覆盖初始化自动查询、查询场景
2. 覆盖业务单元、期间、币种、模板、showZero 过滤场景
3. 覆盖 checks、warnings、报表行展示与跳转按钮场景
4. 覆盖空结果、异常结果、未知编码/现金划转/启发式分类提示场景
5. 对 BUS 未明确项显式标记“待确认”
