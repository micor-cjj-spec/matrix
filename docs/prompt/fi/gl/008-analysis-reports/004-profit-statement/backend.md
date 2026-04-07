# 利润表后端提示词

## 目标
结合 `docs/bus/fi/gl/008-analysis-reports/004-profit-statement/` 下业务文档，以及当前已确认前端接入事实，输出利润表场景的后端补齐提示词。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `api.md`
- `docs/prompt/fi/gl/008-analysis-reports/004-profit-statement/dictionary.md`
- `docs/prompt/fi/gl/008-analysis-reports/004-profit-statement/api.md`

同时结合当前已确认实现：
- 前端已接入利润表查询接口调用
- 本轮仓库检索中未定位到对应后端 controller / service 实现文件

## 输出要求
1. 明确写明“后端内部实现文件本轮未定位，不展开具体类名和内部逻辑”
2. 输出后续后端补齐时应明确的内容：利润表项目取数、本期金额、本年累计金额、warnings/checks 生成口径
3. 说明期间范围与模板、币种参数的一致性要求
4. 不虚构具体 controller / service / mapper 名称
5. 对 BUS 未明确项显式标记“待确认”
