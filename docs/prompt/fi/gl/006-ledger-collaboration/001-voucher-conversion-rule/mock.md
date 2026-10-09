# 凭证折算规则样例提示词

## 目标
根据 `docs/bus/fi/gl/006-ledger-collaboration/001-voucher-conversion-rule/` 下业务文档，以及当前已确认实现，输出凭证折算规则场景的样例数据与 mock 提示词。

## 输入上下文
请结合以下文档：
- `fields.md`
- `page.md`
- `api.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/001-voucher-conversion-rule/dictionary.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/001-voucher-conversion-rule/api.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/001-voucher-conversion-rule/frontend.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/001-voucher-conversion-rule/test.md`

## 输出要求
1. 输出默认查询样例、汇总卡片样例、warnings 样例、规则清单样例
2. 输出不同往来类型、关键字过滤、空结果、异常结果等样例
3. 输出跳转到应收/应付业务单据页面参数样例
4. 对 BUS 未明确项显式标记“待确认”
