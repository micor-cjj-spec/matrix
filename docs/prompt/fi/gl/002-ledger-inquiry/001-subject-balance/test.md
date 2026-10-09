# 科目余额表测试提示词

## 目标
根据 `docs/bus/fi/gl/002-ledger-inquiry/001-subject-balance/` 下业务文档，以及当前前后端已确认实现，输出科目余额表场景的测试与验证提示词。

## 输入上下文
请结合以下文档：
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/001-subject-balance/dictionary.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/001-subject-balance/api.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/001-subject-balance/backend.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/001-subject-balance/frontend.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/001-subject-balance/sql.md`

## 输出要求
1. 覆盖 query 回填、默认查询、查询、重置场景
2. 覆盖科目下拉加载、warnings 展示、summary 展示、表格展示场景
3. 覆盖空结果、异常结果、不同科目编码过滤场景
4. 输出 API/UI/E2E 分层测试建议
5. 对 BUS 未明确项显式标记“待确认”
