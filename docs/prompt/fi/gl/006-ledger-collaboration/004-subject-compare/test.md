# 科目余额对照测试提示词

## 目标
根据 `docs/bus/fi/gl/006-ledger-collaboration/004-subject-compare/` 下业务文档，以及当前前后端已确认实现，输出科目余额对照场景的测试与验证提示词。

## 输入上下文
请结合以下文档：
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/004-subject-compare/dictionary.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/004-subject-compare/api.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/004-subject-compare/backend.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/004-subject-compare/frontend.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/004-subject-compare/sql.md`

## 输出要求
1. 覆盖初始化自动查询与查询场景
2. 覆盖开始日期、结束日期、科目编码、仅看差异过滤场景
3. 覆盖汇总卡片、warnings、表格展示场景
4. 覆盖跳转到科目余额表页面、空结果、异常结果场景
5. 对 BUS 未明确项显式标记“待确认”
