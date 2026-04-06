# 现金流通知单勾稽样例提示词

## 目标
根据 `docs/bus/fi/gl/005-internal-notice/004-cashflow-notice-check/` 下业务文档，以及当前已确认实现，输出现金流通知单勾稽场景的样例数据与 mock 提示词。

## 输入上下文
请结合以下文档：
- `fields.md`
- `page.md`
- `api.md`
- `docs/prompt/fi/gl/005-internal-notice/004-cashflow-notice-check/dictionary.md`
- `docs/prompt/fi/gl/005-internal-notice/004-cashflow-notice-check/api.md`
- `docs/prompt/fi/gl/005-internal-notice/004-cashflow-notice-check/frontend.md`
- `docs/prompt/fi/gl/005-internal-notice/004-cashflow-notice-check/test.md`

## 输出要求
1. 输出默认查询样例、汇总卡片样例、warnings 样例、勾稽结果样例
2. 输出不同勾稽状态、空结果、异常结果等样例
3. 输出跳转到凭证页参数样例
4. 对 BUS 未明确项显式标记“待确认”
