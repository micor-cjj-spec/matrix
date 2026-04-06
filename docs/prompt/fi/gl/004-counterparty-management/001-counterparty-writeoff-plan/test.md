# 往来核销方案测试提示词

## 目标
根据 `docs/bus/fi/gl/004-counterparty-management/001-counterparty-writeoff-plan/` 下业务文档，以及当前前后端已确认实现，输出往来核销方案场景的测试与验证提示词。

## 输入上下文
请结合以下文档：
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`
- `docs/prompt/fi/gl/004-counterparty-management/001-counterparty-writeoff-plan/dictionary.md`
- `docs/prompt/fi/gl/004-counterparty-management/001-counterparty-writeoff-plan/api.md`
- `docs/prompt/fi/gl/004-counterparty-management/001-counterparty-writeoff-plan/backend.md`
- `docs/prompt/fi/gl/004-counterparty-management/001-counterparty-writeoff-plan/frontend.md`
- `docs/prompt/fi/gl/004-counterparty-management/001-counterparty-writeoff-plan/sql.md`

## 输出要求
1. 覆盖查询方案场景
2. 覆盖往来类型、往来方、统计日期、仅已审核过滤场景
3. 覆盖 warnings、统计字段、方案记录展示场景
4. 覆盖“去自动核销”跳转、空结果、异常结果场景
5. 对 BUS 未明确项显式标记“待确认”
