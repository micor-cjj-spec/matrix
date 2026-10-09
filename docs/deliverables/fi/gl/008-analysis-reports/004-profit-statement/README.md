# 利润表交付物说明

## 1. 对应 BUS 场景
- BUS 路径：`docs/bus/fi/gl/008-analysis-reports/004-profit-statement/`
- 业务名称：利润表
- 业务定位：用于按业务单元、期间范围、模板和币种查看本期金额与本年累计金额，展示收入、成本、费用和利润结构。

## 2. 与代码对齐结论
当前已确认：
- 前端页面：`ProfitStatementView.vue`
- 前端已接入接口：`GET /profit-statement`
- 前端 API：`financialReportApi.fetchProfitStatement`

同时 BUS 明确说明：
- 本轮仓库检索中未定位到对应后端 controller / service 实现文件

因此，本交付物按“前端已接入、后端内部实现未展开”的边界编写，不虚构后端内部类与表结构细节。

## 3. 对应 Prompt 来源
- `docs/prompt/fi/gl/008-analysis-reports/004-profit-statement/dictionary.md`
- `docs/prompt/fi/gl/008-analysis-reports/004-profit-statement/api.md`
- `docs/prompt/fi/gl/008-analysis-reports/004-profit-statement/backend.md`
- `docs/prompt/fi/gl/008-analysis-reports/004-profit-statement/frontend.md`
- `docs/prompt/fi/gl/008-analysis-reports/004-profit-statement/sql.md`
- `docs/prompt/fi/gl/008-analysis-reports/004-profit-statement/test.md`
- `docs/prompt/fi/gl/008-analysis-reports/004-profit-statement/review.md`
- `docs/prompt/fi/gl/008-analysis-reports/004-profit-statement/ops.md`
- `docs/prompt/fi/gl/008-analysis-reports/004-profit-statement/mock.md`

## 4. 交付物清单
- `dictionary.md`：利润表字段与期间口径说明
- `api.md`：利润表接口正式说明
- `sql.md`：主表查询口径说明
- `test.md`：测试验证说明
- `ops.md`：发布运维说明
- `review.md`：一致性评审清单
- `mock.md`：样例与联调数据说明

## 5. 风险与注意事项
- 当前文档不展开后端内部 controller / service / mapper / 表结构细节
- 本期金额与本年累计金额的计算口径必须以前端接口返回事实为准
- 当前页面未接入项目下钻弹窗，不应误写为已具备下钻能力
