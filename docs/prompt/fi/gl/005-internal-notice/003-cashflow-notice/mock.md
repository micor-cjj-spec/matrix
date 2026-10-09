# 现金流通知单样例提示词

## 目标
根据 `docs/bus/fi/gl/005-internal-notice/003-cashflow-notice/` 下业务文档，以及当前已确认实现，输出现金流通知单场景的样例数据与 mock 提示词。

## 输入上下文
请结合以下文档：
- `fields.md`
- `page.md`
- `api.md`
- `docs/prompt/fi/gl/005-internal-notice/003-cashflow-notice/dictionary.md`
- `docs/prompt/fi/gl/005-internal-notice/003-cashflow-notice/api.md`
- `docs/prompt/fi/gl/005-internal-notice/003-cashflow-notice/frontend.md`
- `docs/prompt/fi/gl/005-internal-notice/003-cashflow-notice/test.md`

## 输出要求
1. 输出默认查询样例、汇总卡片样例、warnings 样例、通知清单样例、生成结果样例
2. 输出不同通知状态、紧急程度、问题来源、空结果、异常结果等样例
3. 输出跳转到凭证页与现金流通知单勾稽页面参数样例
4. 对 BUS 未明确项显式标记“待确认”
