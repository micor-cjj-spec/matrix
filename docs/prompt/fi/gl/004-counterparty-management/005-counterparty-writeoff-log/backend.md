# 往来核销日志后端提示词

## 目标
结合 `docs/bus/fi/gl/004-counterparty-management/005-counterparty-writeoff-log/` 下业务文档，以及当前后端已确认实现，输出往来核销日志场景的后端实现建议与补齐提示词。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `api.md`
- `docs/prompt/fi/gl/004-counterparty-management/005-counterparty-writeoff-log/dictionary.md`
- `docs/prompt/fi/gl/004-counterparty-management/005-counterparty-writeoff-log/api.md`

同时结合当前已确认实现：
- 查询服务：`BizfiFiArapManageServiceImpl.writeoffLog`
- 读取 `BizfiFiArapWriteoffLog / BizfiFiArapWriteoffLink` 生成结果
- 返回 `records`、`linkDetails`、`warnings` 与统计字段

## 输出要求
1. 说明当前后端日志追溯能力与待补齐项
2. 输出批次摘要、链接明细、核销金额、操作人、操作时间等计算逻辑建议
3. 说明 docTypeRoot、counterparty、planCode、startDate、endDate 过滤逻辑
4. 说明性能、空结果与异常结果处理建议
5. 对 BUS 未明确项显式标记“待确认”
