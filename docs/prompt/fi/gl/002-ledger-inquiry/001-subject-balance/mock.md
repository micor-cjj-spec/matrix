# 科目余额表样例提示词

## 目标
根据 `docs/bus/fi/gl/002-ledger-inquiry/001-subject-balance/` 下业务文档，以及当前已确认实现，输出科目余额表场景的样例数据与 mock 提示词。

## 输入上下文
请结合以下文档：
- `fields.md`
- `page.md`
- `api.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/001-subject-balance/dictionary.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/001-subject-balance/api.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/001-subject-balance/frontend.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/001-subject-balance/test.md`

## 输出要求
1. 输出默认查询样例、summary 样例、warnings 样例、余额表行样例
2. 输出带 accountCode 过滤、空结果、异常结果等样例
3. 输出路由 query 样例与接口请求返回样例
4. 对 BUS 未明确项显式标记“待确认”
