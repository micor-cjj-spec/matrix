# 账龄分析表后端提示词

## 目标
结合 `docs/bus/fi/gl/004-counterparty-management/006-aging-analysis/` 下业务文档，以及当前后端已确认实现，输出账龄分析表场景的后端实现建议与补齐提示词。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `api.md`
- `docs/prompt/fi/gl/004-counterparty-management/006-aging-analysis/dictionary.md`
- `docs/prompt/fi/gl/004-counterparty-management/006-aging-analysis/api.md`

同时结合当前已确认实现：
- 查询服务：`BizfiFiArapManageServiceImpl.agingAnalysis`
- 基于往来单据、核销链接、往来方信用额度主数据生成结果
- 返回 `rows`、`warnings` 与统计字段

## 输出要求
1. 说明当前后端账龄分析能力与待补齐项
2. 输出未核销余额、账龄分桶、信用额度、逾期阈值、风险状态计算逻辑建议
3. 说明 docTypeRoot、asOfDate 过滤逻辑
4. 说明性能、空结果与异常结果处理建议
5. 对 BUS 未明确项显式标记“待确认”
