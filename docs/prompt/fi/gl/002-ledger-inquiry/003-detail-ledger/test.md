# 明细分类账测试提示词

## 目标
根据 `docs/bus/fi/gl/002-ledger-inquiry/003-detail-ledger/` 下业务文档，以及当前前后端已确认实现，输出明细分类账场景的测试与验证提示词。

## 输入上下文
请结合以下文档：
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/003-detail-ledger/dictionary.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/003-detail-ledger/api.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/003-detail-ledger/backend.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/003-detail-ledger/frontend.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/003-detail-ledger/sql.md`

## 输出要求
1. 覆盖初始化自动查询、查询、重置场景
2. 覆盖科目选项加载、warnings 展示、summary 展示、明细流水表格展示场景
3. 覆盖空结果、异常结果、不同 accountCode 过滤场景
4. 覆盖滚动余额与余额方向展示校验
5. 对 BUS 未明确项显式标记“待确认”
