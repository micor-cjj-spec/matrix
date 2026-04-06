# 日报表测试提示词

## 目标
根据 `docs/bus/fi/gl/002-ledger-inquiry/004-daily-report/` 下业务文档，以及当前前后端已确认实现，输出日报表场景的测试与验证提示词。

## 输入上下文
请结合以下文档：
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/004-daily-report/dictionary.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/004-daily-report/api.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/004-daily-report/backend.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/004-daily-report/frontend.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/004-daily-report/sql.md`

## 输出要求
1. 覆盖初始化自动查询、查询、重置场景
2. 覆盖科目选项加载、warnings 展示、summary 展示、日报表表格展示场景
3. 覆盖空结果、异常结果、不同 accountCode 过滤场景
4. 覆盖按日统计、凭证数、滚动余额展示校验
5. 对 BUS 未明确项显式标记“待确认”
