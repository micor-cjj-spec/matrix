# 科目余额对照样例提示词

## 目标
根据 `docs/bus/fi/gl/006-ledger-collaboration/004-subject-compare/` 下业务文档，以及当前已确认实现，输出科目余额对照场景的样例数据与 mock 提示词。

## 输入上下文
请结合以下文档：
- `fields.md`
- `page.md`
- `api.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/004-subject-compare/dictionary.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/004-subject-compare/api.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/004-subject-compare/frontend.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/004-subject-compare/test.md`

## 输出要求
1. 输出默认查询样例、汇总卡片样例、warnings 样例、科目对照结果样例
2. 输出仅看差异、指定科目过滤、空结果、异常结果等样例
3. 输出跳转到科目余额表页面参数样例
4. 对 BUS 未明确项显式标记“待确认”
