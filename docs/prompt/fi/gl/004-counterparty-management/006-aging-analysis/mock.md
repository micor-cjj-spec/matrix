# 账龄分析表样例提示词

## 目标
根据 `docs/bus/fi/gl/004-counterparty-management/006-aging-analysis/` 下业务文档，以及当前已确认实现，输出账龄分析表场景的样例数据与 mock 提示词。

## 输入上下文
请结合以下文档：
- `fields.md`
- `page.md`
- `api.md`
- `docs/prompt/fi/gl/004-counterparty-management/006-aging-analysis/dictionary.md`
- `docs/prompt/fi/gl/004-counterparty-management/006-aging-analysis/api.md`
- `docs/prompt/fi/gl/004-counterparty-management/006-aging-analysis/frontend.md`
- `docs/prompt/fi/gl/004-counterparty-management/006-aging-analysis/test.md`

## 输出要求
1. 输出默认查询样例、汇总卡片样例、warnings 样例、账龄结果样例
2. 输出不同账龄分桶、风险状态、空结果、异常结果等样例
3. 对 BUS 未明确项显式标记“待确认”
