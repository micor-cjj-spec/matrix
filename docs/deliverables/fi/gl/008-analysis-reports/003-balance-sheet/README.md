# 资产负债表交付物说明

## 1. 对应 BUS 场景
- BUS 路径：`docs/bus/fi/gl/008-analysis-reports/003-balance-sheet/`
- 业务名称：资产负债表
- 业务定位：用于按业务单元、期间、模板和币种查看资产、负债及所有者权益项目的期末余额和年初余额，并支持按报表项目下钻到会计科目。

## 2. 与代码对齐结论
当前已确认：
- 前端页面：`BalanceSheetView.vue`
- 前端已接入接口：`GET /balance-sheet`、`GET /balance-sheet/drill`
- 前端 API：`financialReportApi.fetchBalanceSheet`、`financialReportApi.fetchBalanceSheetDrill`

同时 BUS 明确说明：
- 本轮仓库检索中未定位到对应后端 controller / service 实现文件

因此，本交付物按“前端已接入、后端内部实现未展开”的边界编写，不虚构后端内部类与表结构细节。

## 3. 对应 Prompt 来源
- `docs/prompt/fi/gl/008-analysis-reports/003-balance-sheet/dictionary.md`
- `docs/prompt/fi/gl/008-analysis-reports/003-balance-sheet/api.md`
- `docs/prompt/fi/gl/008-analysis-reports/003-balance-sheet/backend.md`
- `docs/prompt/fi/gl/008-analysis-reports/003-balance-sheet/frontend.md`
- `docs/prompt/fi/gl/008-analysis-reports/003-balance-sheet/sql.md`
- `docs/prompt/fi/gl/008-analysis-reports/003-balance-sheet/test.md`
- `docs/prompt/fi/gl/008-analysis-reports/003-balance-sheet/review.md`
- `docs/prompt/fi/gl/008-analysis-reports/003-balance-sheet/ops.md`
- `docs/prompt/fi/gl/008-analysis-reports/003-balance-sheet/mock.md`

## 4. 交付物清单
- `dictionary.md`：报表字段与平衡口径说明
- `api.md`：资产负债表接口正式说明
- `sql.md`：主表与下钻查询口径说明
- `test.md`：测试验证说明
- `ops.md`：发布运维说明
- `review.md`：一致性评审清单
- `mock.md`：样例与联调数据说明

## 5. 风险与注意事项
- 当前文档不展开后端内部 controller / service / mapper / 表结构细节
- 资产总计与负债和所有者权益总计的平衡校验口径必须以前端接口返回事实为准
- 下钻口径必须与主表项目口径保持一致
