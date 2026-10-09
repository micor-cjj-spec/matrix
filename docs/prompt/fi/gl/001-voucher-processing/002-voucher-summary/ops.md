# 凭证汇总表运维提示词

## 目标
根据 `docs/bus/fi/gl/001-voucher-processing/002-voucher-summary/` 下业务文档，以及当前已确认实现，输出凭证汇总表场景的发布、运维、监控与问题处理提示词。

## 输入上下文
请结合以下文档：
- `flow.md`
- `rules.md`
- `api.md`
- `docs/prompt/fi/gl/001-voucher-processing/002-voucher-summary/backend.md`
- `docs/prompt/fi/gl/001-voucher-processing/002-voucher-summary/frontend.md`
- `docs/prompt/fi/gl/001-voucher-processing/002-voucher-summary/sql.md`
- `docs/prompt/fi/gl/001-voucher-processing/002-voucher-summary/test.md`
- `docs/prompt/fi/gl/001-voucher-processing/002-voucher-summary/review.md`

## 输出要求
1. 输出发布依赖检查项（前端/后端）
2. 输出查询性能、聚合接口、warnings 相关监控关注点
3. 输出跳转兼容性与接口兼容性注意项
4. 输出回滚与异常排查建议
5. 对 BUS 未明确项显式标记“待确认”
