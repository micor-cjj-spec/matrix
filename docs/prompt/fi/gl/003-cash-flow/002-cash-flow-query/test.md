# 现金流量查询测试提示词

## 目标
根据 `docs/bus/fi/gl/003-cash-flow/002-cash-flow-query/` 下业务文档，以及当前前后端已确认实现，输出现金流量查询场景的测试与验证提示词。

## 输入上下文
请结合以下文档：
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`
- `docs/prompt/fi/gl/003-cash-flow/002-cash-flow-query/dictionary.md`
- `docs/prompt/fi/gl/003-cash-flow/002-cash-flow-query/api.md`
- `docs/prompt/fi/gl/003-cash-flow/002-cash-flow-query/backend.md`
- `docs/prompt/fi/gl/003-cash-flow/002-cash-flow-query/frontend.md`
- `docs/prompt/fi/gl/003-cash-flow/002-cash-flow-query/sql.md`

## 输出要求
1. 覆盖初始化自动查询、查询、重置场景
2. 覆盖业务单元、期间、现金流项目、活动分类、识别方式、科目编码关键字、关键字过滤场景
3. 覆盖状态条、warnings、表格展示、查看凭证跳转场景
4. 覆盖空结果、异常结果、未知编码/多编码复核/现金划转提示场景
5. 对 BUS 未明确项显式标记“待确认”
