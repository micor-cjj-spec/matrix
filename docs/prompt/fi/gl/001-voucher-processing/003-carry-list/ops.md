# 结转清单运维提示词

## 目标
根据 `docs/bus/fi/gl/001-voucher-processing/003-carry-list/` 下业务文档，以及当前已确认实现，输出结转清单场景的发布、运维、监控与问题处理提示词。

## 输入上下文
请结合以下文档：
- `flow.md`
- `rules.md`
- `api.md`
- `docs/prompt/fi/gl/001-voucher-processing/003-carry-list/backend.md`
- `docs/prompt/fi/gl/001-voucher-processing/003-carry-list/frontend.md`
- `docs/prompt/fi/gl/001-voucher-processing/003-carry-list/sql.md`
- `docs/prompt/fi/gl/001-voucher-processing/003-carry-list/test.md`
- `docs/prompt/fi/gl/001-voucher-processing/003-carry-list/review.md`

## 输出要求
1. 输出发布依赖检查项（前端/后端/下拉依赖）
2. 输出检查项聚合接口、warnings、跳转相关监控关注点
3. 输出业务单元列表加载与期间口径的兼容性注意项
4. 输出回滚与异常排查建议
5. 对 BUS 未明确项显式标记“待确认”
