# 往来自动核销SQL提示词

## 目标
根据 `docs/bus/fi/gl/004-counterparty-management/002-counterparty-auto-writeoff/` 下业务文档，以及当前已确认实现，输出往来自动核销场景的 SQL 与查询设计提示词。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `rules.md`
- `api.md`
- `docs/prompt/fi/gl/004-counterparty-management/002-counterparty-auto-writeoff/dictionary.md`
- `docs/prompt/fi/gl/004-counterparty-management/002-counterparty-auto-writeoff/backend.md`

同时结合当前已确认实现：
- 基于已审核 AR/AP 单据生成匹配并写入 `BizfiFiArapWriteoffLog / BizfiFiArapWriteoffLink`
- 预览接口复用方案查询，执行接口负责落库

## 输出要求
1. 输出预览查询与执行落库所需数据来源口径
2. 输出方案号、日志号、核销链接、核销金额相关表结构与字段建议
3. 输出 docTypeRoot、counterparty、asOfDate 过滤口径
4. 输出事务一致性、索引与性能优化建议
5. 对 BUS 未明确项显式标记“待确认”
