# 往来自动核销样例提示词

## 目标
根据 `docs/bus/fi/gl/004-counterparty-management/002-counterparty-auto-writeoff/` 下业务文档，以及当前已确认实现，输出往来自动核销场景的样例数据与 mock 提示词。

## 输入上下文
请结合以下文档：
- `fields.md`
- `page.md`
- `api.md`
- `docs/prompt/fi/gl/004-counterparty-management/002-counterparty-auto-writeoff/dictionary.md`
- `docs/prompt/fi/gl/004-counterparty-management/002-counterparty-auto-writeoff/api.md`
- `docs/prompt/fi/gl/004-counterparty-management/002-counterparty-auto-writeoff/frontend.md`
- `docs/prompt/fi/gl/004-counterparty-management/002-counterparty-auto-writeoff/test.md`

## 输出要求
1. 输出默认预览样例、统计卡片样例、warnings 样例、执行结果样例
2. 输出空结果、异常结果、重复执行等样例
3. 输出从方案页带入参数样例与执行成功提示区样例
4. 对 BUS 未明确项显式标记“待确认”
