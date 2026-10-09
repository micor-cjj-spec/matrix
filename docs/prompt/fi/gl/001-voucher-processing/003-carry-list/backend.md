# 结转清单后端提示词

## 目标
结合 `docs/bus/fi/gl/001-voucher-processing/003-carry-list/` 下业务文档，以及当前后端已确认实现，输出结转清单场景的后端实现建议与补齐提示词。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `api.md`
- `docs/prompt/fi/gl/001-voucher-processing/003-carry-list/dictionary.md`
- `docs/prompt/fi/gl/001-voucher-processing/003-carry-list/api.md`

同时结合当前已确认实现：
- 聚合服务：`BizfiFiVoucherAnalysisServiceImpl.carryList`
- 汇总组织财务参数、期间、默认凭证字、基础资料健康检查、相关凭证
- 返回 warnings、tasks、relatedVouchers

## 输出要求
1. 说明当前后端聚合能力与待补齐项
2. 输出业务单元、期间、基础资料健康检查等聚合逻辑建议
3. 说明 `tasks`、`relatedVouchers`、`warnings` 的生成口径
4. 说明性能、空结果与异常结果处理建议
5. 对 BUS 未明确项显式标记“待确认”
