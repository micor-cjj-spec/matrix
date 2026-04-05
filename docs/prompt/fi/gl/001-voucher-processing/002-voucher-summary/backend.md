# 凭证汇总表后端提示词

## 目标
结合 `docs/bus/fi/gl/001-voucher-processing/002-voucher-summary/` 下业务文档，以及当前后端已确认实现，输出凭证汇总表场景的后端实现建议与补齐提示词。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `api.md`
- `docs/prompt/fi/gl/001-voucher-processing/002-voucher-summary/dictionary.md`
- `docs/prompt/fi/gl/001-voucher-processing/002-voucher-summary/api.md`

同时结合当前已确认实现：
- 聚合服务：`BizfiFiVoucherAnalysisServiceImpl.summary`
- 数据来源：`BizfiFiVoucher` 主表
- 不读取分录表
- 支持 warnings 返回

## 输出要求
1. 说明当前后端聚合能力与待补齐项
2. 输出查询参数校验、聚合逻辑、warnings 生成逻辑建议
3. 说明统计口径、状态分桶、金额汇总、日期分组逻辑
4. 说明性能、索引依赖、空结果与异常结果处理建议
5. 对 BUS 未明确项显式标记“待确认”
