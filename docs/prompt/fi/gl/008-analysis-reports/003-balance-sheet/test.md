# 资产负债表测试提示词

## 目标
根据 `docs/bus/fi/gl/008-analysis-reports/003-balance-sheet/` 下业务文档，以及当前前端已确认实现，输出资产负债表场景的测试与验证提示词。

## 输入上下文
请结合以下文档：
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`
- `docs/prompt/fi/gl/008-analysis-reports/003-balance-sheet/dictionary.md`
- `docs/prompt/fi/gl/008-analysis-reports/003-balance-sheet/api.md`
- `docs/prompt/fi/gl/008-analysis-reports/003-balance-sheet/backend.md`
- `docs/prompt/fi/gl/008-analysis-reports/003-balance-sheet/frontend.md`
- `docs/prompt/fi/gl/008-analysis-reports/003-balance-sheet/sql.md`

## 输出要求
1. 覆盖初始化自动查询与查询场景
2. 覆盖业务单元、期间、币种、报表模板、showZero 过滤场景
3. 覆盖元信息条、汇总卡片、warnings、checks、主表展示场景
4. 覆盖下钻弹窗、空结果、异常结果场景
5. 明确标注“后端内部实现文件本轮未定位”并对未明确项标记“待确认”
