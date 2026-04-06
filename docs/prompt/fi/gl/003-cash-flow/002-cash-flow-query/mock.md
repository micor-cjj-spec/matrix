# 现金流量查询样例提示词

## 目标
根据 `docs/bus/fi/gl/003-cash-flow/002-cash-flow-query/` 下业务文档，以及当前已确认实现，输出现金流量查询场景的样例数据与 mock 提示词。

## 输入上下文
请结合以下文档：
- `fields.md`
- `page.md`
- `api.md`
- `docs/prompt/fi/gl/003-cash-flow/002-cash-flow-query/dictionary.md`
- `docs/prompt/fi/gl/003-cash-flow/002-cash-flow-query/api.md`
- `docs/prompt/fi/gl/003-cash-flow/002-cash-flow-query/frontend.md`
- `docs/prompt/fi/gl/003-cash-flow/002-cash-flow-query/test.md`

## 输出要求
1. 输出默认查询样例、统计卡片样例、状态条样例、warnings 样例、追踪记录样例
2. 输出带现金流项目、活动分类、识别方式、关键字过滤、空结果、异常结果等样例
3. 输出查看凭证跳转参数样例
4. 对 BUS 未明确项显式标记“待确认”
