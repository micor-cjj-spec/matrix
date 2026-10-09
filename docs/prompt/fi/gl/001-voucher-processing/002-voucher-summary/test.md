# 凭证汇总表测试提示词

## 目标
根据 `docs/bus/fi/gl/001-voucher-processing/002-voucher-summary/` 下业务文档，以及当前前后端已确认实现，输出凭证汇总表场景的测试与验证提示词。

## 输入上下文
请结合以下文档：
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`
- `docs/prompt/fi/gl/001-voucher-processing/002-voucher-summary/dictionary.md`
- `docs/prompt/fi/gl/001-voucher-processing/002-voucher-summary/api.md`
- `docs/prompt/fi/gl/001-voucher-processing/002-voucher-summary/backend.md`
- `docs/prompt/fi/gl/001-voucher-processing/002-voucher-summary/frontend.md`
- `docs/prompt/fi/gl/001-voucher-processing/002-voucher-summary/sql.md`

同时结合当前已确认实现：默认本月查询、warnings 区、表格聚合结果、查看凭证跳转。

## 输出要求
1. 覆盖默认查询、查询、重置、空结果、异常结果场景
2. 覆盖日期区间、状态、摘要关键字过滤场景
3. 覆盖统计口径、warnings 展示、查看凭证跳转场景
4. 输出 API/UI/E2E 分层测试建议
5. 对 BUS 未明确项显式标记“待确认”
