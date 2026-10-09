# 往来对账单样例提示词

## 目标
根据 `docs/bus/fi/gl/004-counterparty-management/003-counterparty-statement/` 下业务文档，以及当前已确认实现，输出往来对账单场景的样例数据与 mock 提示词。

## 输入上下文
请结合以下文档：
- `fields.md`
- `page.md`
- `api.md`
- `docs/prompt/fi/gl/004-counterparty-management/003-counterparty-statement/dictionary.md`
- `docs/prompt/fi/gl/004-counterparty-management/003-counterparty-statement/api.md`
- `docs/prompt/fi/gl/004-counterparty-management/003-counterparty-statement/frontend.md`
- `docs/prompt/fi/gl/004-counterparty-management/003-counterparty-statement/test.md`

## 输出要求
1. 输出默认查询样例、汇总卡片样例、warnings 样例、对账明细样例、最近批次样例
2. 输出仅看未核销、空结果、异常结果等样例
3. 输出关联凭证跳转与核销日志跳转参数样例
4. 对 BUS 未明确项显式标记“待确认”
