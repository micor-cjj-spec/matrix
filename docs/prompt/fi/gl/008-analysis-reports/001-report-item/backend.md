# 报表项目后端提示词

## 目标
结合 `docs/bus/fi/gl/008-analysis-reports/001-report-item/` 下业务文档，以及当前后端已确认实现，输出报表项目场景的后端实现建议与补齐提示词。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `api.md`
- `docs/prompt/fi/gl/008-analysis-reports/001-report-item/dictionary.md`
- `docs/prompt/fi/gl/008-analysis-reports/001-report-item/api.md`

同时结合当前已确认实现：
- 后端：`BizfiFiReportItemController / ServiceImpl`
- 提供 list/tree/detail/create/update/delete 能力
- 基于报表项目表完成查询与基础维护

## 输出要求
1. 说明当前后端查询与基础维护能力
2. 输出分页查询、树查询、详情、写操作的数据一致性建议
3. 说明模板、项目编码、项目名称、层级、排序等核心校验建议
4. 说明前端未接入写入口时，后端接口的保留边界
5. 对 BUS 未明确项显式标记“待确认”
