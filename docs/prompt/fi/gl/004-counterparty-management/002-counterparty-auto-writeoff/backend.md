# 往来自动核销后端提示词

## 目标
结合 `docs/bus/fi/gl/004-counterparty-management/002-counterparty-auto-writeoff/` 下业务文档，以及当前后端已确认实现，输出往来自动核销场景的后端实现建议与补齐提示词。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `api.md`
- `docs/prompt/fi/gl/004-counterparty-management/002-counterparty-auto-writeoff/dictionary.md`
- `docs/prompt/fi/gl/004-counterparty-management/002-counterparty-auto-writeoff/api.md`

同时结合当前已确认实现：
- 执行服务：`BizfiFiArapManageServiceImpl.autoWriteoff`
- 落库表：`BizfiFiArapWriteoffLog / BizfiFiArapWriteoffLink`
- 预览基于方案接口，执行基于已审核单据生成匹配并落库

## 输出要求
1. 说明当前后端自动核销能力与待补齐项
2. 输出匹配、核销金额、剩余金额、方案号与日志号生成逻辑建议
3. 说明 docTypeRoot、counterparty、asOfDate、operator 参数处理逻辑
4. 说明事务边界、落库一致性、性能、空结果与异常结果处理建议
5. 对 BUS 未明确项显式标记“待确认”
