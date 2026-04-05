# 明细分类账样例提示词

## 目标
根据 `docs/bus/fi/gl/002-ledger-inquiry/003-detail-ledger/` 下业务文档，以及当前已确认实现，输出明细分类账场景的样例数据与 mock 提示词。

## 输入上下文
请结合以下文档：
- `fields.md`
- `page.md`
- `api.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/003-detail-ledger/dictionary.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/003-detail-ledger/api.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/003-detail-ledger/frontend.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/003-detail-ledger/test.md`

## 输出要求
1. 输出默认查询样例、summary 样例、warnings 样例、明细流水行样例
2. 输出带 accountCode 过滤、空结果、异常结果等样例
3. 输出接口请求与返回样例
4. 对 BUS 未明确项显式标记“待确认”
