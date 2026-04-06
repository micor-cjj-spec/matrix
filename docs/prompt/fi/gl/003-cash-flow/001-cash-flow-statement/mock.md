# 现金流量表样例提示词

## 目标
根据 `docs/bus/fi/gl/003-cash-flow/001-cash-flow-statement/` 下业务文档，以及当前已确认实现，输出现金流量表场景的样例数据与 mock 提示词。

## 输入上下文
请结合以下文档：
- `fields.md`
- `page.md`
- `api.md`
- `docs/prompt/fi/gl/003-cash-flow/001-cash-flow-statement/dictionary.md`
- `docs/prompt/fi/gl/003-cash-flow/001-cash-flow-statement/api.md`
- `docs/prompt/fi/gl/003-cash-flow/001-cash-flow-statement/frontend.md`
- `docs/prompt/fi/gl/003-cash-flow/001-cash-flow-statement/test.md`

## 输出要求
1. 输出默认查询样例、模板样例、checks 样例、warnings 样例、报表行样例
2. 输出带币种、模板、showZero 过滤、空结果、异常结果等样例
3. 输出“现金流量查询”与“补充资料”跳转参数样例
4. 对 BUS 未明确项显式标记“待确认”
