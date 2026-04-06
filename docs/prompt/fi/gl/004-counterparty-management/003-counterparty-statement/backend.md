# 往来对账单后端提示词

## 目标
结合 `docs/bus/fi/gl/004-counterparty-management/003-counterparty-statement/` 下业务文档，以及当前后端已确认实现，输出往来对账单场景的后端实现建议与补齐提示词。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `api.md`
- `docs/prompt/fi/gl/004-counterparty-management/003-counterparty-statement/dictionary.md`
- `docs/prompt/fi/gl/004-counterparty-management/003-counterparty-statement/api.md`

同时结合当前已确认实现：
- 查询服务：`BizfiFiArapManageServiceImpl.statement`
- 基于往来单据和已落库核销日志/链接生成结果
- 返回 `rows`、`recentLogs`、`warnings` 与统计字段

## 输出要求
1. 说明当前后端对账能力与待补齐项
2. 输出原额、已核销金额、未核销金额、核销率、最近批次摘要的计算逻辑建议
3. 说明 docTypeRoot、counterparty、asOfDate、openOnly 过滤逻辑
4. 说明性能、空结果与异常结果处理建议
5. 对 BUS 未明确项显式标记“待确认”
