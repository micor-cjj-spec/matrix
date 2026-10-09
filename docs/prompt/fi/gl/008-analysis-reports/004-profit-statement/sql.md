# 利润表SQL提示词

## 目标
根据 `docs/bus/fi/gl/008-analysis-reports/004-profit-statement/` 下业务文档，以及当前已确认前端接入事实，输出利润表场景的 SQL 与查询设计提示词。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `rules.md`
- `api.md`
- `docs/prompt/fi/gl/008-analysis-reports/004-profit-statement/dictionary.md`
- `docs/prompt/fi/gl/008-analysis-reports/004-profit-statement/backend.md`

同时结合当前已确认实现：
- 查询接口：`GET /profit-statement`
- 前端已接入查询
- 本轮未定位到对应后端实现文件

## 输出要求
1. 输出主表查询所需口径
2. 输出业务单元、期间范围、币种、模板、showZero 的关联关系
3. 输出本期金额、本年累计金额、warnings、checks 的计算口径建议
4. 不虚构具体表名、mapper 名称和内部 SQL 细节
5. 对 BUS 未明确项显式标记“待确认”
