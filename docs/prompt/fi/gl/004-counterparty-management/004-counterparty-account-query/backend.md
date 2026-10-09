# 往来账查询后端提示词

## 目标
结合 `docs/bus/fi/gl/004-counterparty-management/004-counterparty-account-query/` 下业务文档，以及当前后端已确认实现，输出往来账查询场景的后端实现建议与补齐提示词。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `api.md`
- `docs/prompt/fi/gl/004-counterparty-management/004-counterparty-account-query/dictionary.md`
- `docs/prompt/fi/gl/004-counterparty-management/004-counterparty-account-query/api.md`

同时结合当前已确认实现：
- 查询服务：`BizfiFiArapManageServiceImpl.accountQuery`
- 基于往来单据与已落库核销链接生成结果
- 返回 `records`、`warnings` 与统计字段

## 输出要求
1. 说明当前后端单据级查询能力与待补齐项
2. 输出原额、已核销、未核销、核销状态、账龄等计算逻辑建议
3. 说明 docTypeRoot、counterparty、docType、status、openOnly、keyword、asOfDate 过滤逻辑
4. 说明性能、空结果与异常结果处理建议
5. 对 BUS 未明确项显式标记“待确认”
