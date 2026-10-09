# 凭证折算规则测试提示词

## 目标
根据 `docs/bus/fi/gl/006-ledger-collaboration/001-voucher-conversion-rule/` 下业务文档，以及当前前后端已确认实现，输出凭证折算规则场景的测试与验证提示词。

## 输入上下文
请结合以下文档：
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/001-voucher-conversion-rule/dictionary.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/001-voucher-conversion-rule/api.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/001-voucher-conversion-rule/backend.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/001-voucher-conversion-rule/frontend.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/001-voucher-conversion-rule/sql.md`

## 输出要求
1. 覆盖查询场景
2. 覆盖往来类型、关键字过滤场景
3. 覆盖汇总卡片、warnings、表格展示场景
4. 覆盖跳转到应收/应付业务单据页面、空结果、异常结果场景
5. 对 BUS 未明确项显式标记“待确认”
