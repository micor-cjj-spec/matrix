# 现金流量补充资料样例提示词

## 目标
根据 `docs/bus/fi/gl/003-cash-flow/003-cash-flow-supplement/` 下业务文档，以及当前已确认实现，输出现金流量补充资料场景的样例数据与 mock 提示词。

## 输入上下文
请结合以下文档：
- `fields.md`
- `page.md`
- `api.md`
- `docs/prompt/fi/gl/003-cash-flow/003-cash-flow-supplement/dictionary.md`
- `docs/prompt/fi/gl/003-cash-flow/003-cash-flow-supplement/api.md`
- `docs/prompt/fi/gl/003-cash-flow/003-cash-flow-supplement/frontend.md`
- `docs/prompt/fi/gl/003-cash-flow/003-cash-flow-supplement/test.md`

## 输出要求
1. 输出默认查询样例、统计卡片样例、状态条样例、warnings 样例、任务清单样例、分类分布样例、待补录/复核凭证样例
2. 输出空结果、异常结果等样例
3. 输出查看凭证跳转参数样例
4. 对 BUS 未明确项显式标记“待确认”
