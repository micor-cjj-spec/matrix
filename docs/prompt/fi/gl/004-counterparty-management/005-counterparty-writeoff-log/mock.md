# 往来核销日志样例提示词

## 目标
根据 `docs/bus/fi/gl/004-counterparty-management/005-counterparty-writeoff-log/` 下业务文档，以及当前已确认实现，输出往来核销日志场景的样例数据与 mock 提示词。

## 输入上下文
请结合以下文档：
- `fields.md`
- `page.md`
- `api.md`
- `docs/prompt/fi/gl/004-counterparty-management/005-counterparty-writeoff-log/dictionary.md`
- `docs/prompt/fi/gl/004-counterparty-management/005-counterparty-writeoff-log/api.md`
- `docs/prompt/fi/gl/004-counterparty-management/005-counterparty-writeoff-log/frontend.md`
- `docs/prompt/fi/gl/004-counterparty-management/005-counterparty-writeoff-log/test.md`

## 输出要求
1. 输出默认查询样例、汇总卡片样例、warnings 样例、批次样例、明细样例
2. 输出按方案号过滤、空结果、异常结果等样例
3. 输出点击批次行查看明细并带回 `planCode` 的样例
4. 对 BUS 未明确项显式标记“待确认”
