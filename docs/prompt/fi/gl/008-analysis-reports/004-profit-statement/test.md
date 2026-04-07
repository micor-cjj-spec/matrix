# 利润表测试提示词

## 目标
根据 `docs/bus/fi/gl/008-analysis-reports/004-profit-statement/` 下业务文档，以及当前前端已确认实现，输出利润表场景的测试与验证提示词。

## 输入上下文
请结合以下文档：
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`
- `docs/prompt/fi/gl/008-analysis-reports/004-profit-statement/dictionary.md`
- `docs/prompt/fi/gl/008-analysis-reports/004-profit-statement/api.md`
- `docs/prompt/fi/gl/008-analysis-reports/004-profit-statement/backend.md`
- `docs/prompt/fi/gl/008-analysis-reports/004-profit-statement/frontend.md`
- `docs/prompt/fi/gl/008-analysis-reports/004-profit-statement/sql.md`

## 输出要求
1. 覆盖初始化自动查询与查询场景
2. 覆盖业务单元、开始期间、结束期间、币种、模板、showZero 过滤场景
3. 覆盖 warnings、checks、主表展示场景
4. 覆盖空结果、异常结果场景
5. 明确标注“后端内部实现文件本轮未定位”并对未明确项标记“待确认”
