# 往来核销方案样例提示词

## 目标
根据 `docs/bus/fi/gl/004-counterparty-management/001-counterparty-writeoff-plan/` 下业务文档，以及当前已确认实现，输出往来核销方案场景的样例数据与 mock 提示词。

## 输入上下文
请结合以下文档：
- `fields.md`
- `page.md`
- `api.md`
- `docs/prompt/fi/gl/004-counterparty-management/001-counterparty-writeoff-plan/dictionary.md`
- `docs/prompt/fi/gl/004-counterparty-management/001-counterparty-writeoff-plan/api.md`
- `docs/prompt/fi/gl/004-counterparty-management/001-counterparty-writeoff-plan/frontend.md`
- `docs/prompt/fi/gl/004-counterparty-management/001-counterparty-writeoff-plan/test.md`

## 输出要求
1. 输出默认查询样例、统计卡片样例、warnings 样例、方案记录样例
2. 输出带往来类型、往来方、仅已审核过滤、空结果、异常结果等样例
3. 输出“去自动核销”跳转参数样例
4. 对 BUS 未明确项显式标记“待确认”
