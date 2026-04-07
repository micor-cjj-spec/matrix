# 资产负债表数据字典提示词

## 目标
根据 `docs/bus/fi/gl/008-analysis-reports/003-balance-sheet/` 下业务文档，输出资产负债表场景的数据字典、报表口径、平衡口径、校验口径与下钻口径，作为前端、接口、测试的统一语义基础。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`

同时结合当前已确认实现：
- 前端页面：`BalanceSheetView.vue`
- 前端 API：`financialReportApi.fetchBalanceSheet`
- 前端 API：`financialReportApi.fetchBalanceSheetDrill`
- 查询接口：`GET /balance-sheet`
- 下钻接口：`GET /balance-sheet/drill`

## 输出要求
1. 输出查询条件字段字典
2. 输出元信息条、汇总卡片、告警区、校验区、主表区、下钻弹窗字段字典
3. 输出资产总计、负债和所有者权益、差额、平衡状态、warnings、checks 的口径
4. 输出 showZero 对主表展示的影响
5. 对未明确的后端内部实现统一标记“待确认”
