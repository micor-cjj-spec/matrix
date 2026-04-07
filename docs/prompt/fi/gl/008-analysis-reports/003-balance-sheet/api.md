# 资产负债表接口提示词

## 目标
根据 `docs/bus/fi/gl/008-analysis-reports/003-balance-sheet/` 下业务文档，输出资产负债表场景的接口契约提示词，为前端联调、测试与评审提供统一接口口径。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`
- `docs/prompt/fi/gl/008-analysis-reports/003-balance-sheet/dictionary.md`

同时结合当前已确认实现：
- `GET /balance-sheet`
- `GET /balance-sheet/drill`
- 前端 API：`financialReportApi.fetchBalanceSheet`
- 前端 API：`financialReportApi.fetchBalanceSheetDrill`
- 页面：`BalanceSheetView.vue`

## 输出要求
1. 输出主表接口与下钻接口契约
2. 输出主表参数、下钻参数、返回结构、warnings 与 checks 口径
3. 输出主表行结构与下钻明细结构说明
4. 明确标注“前端已接入，后端内部实现文件本轮未定位”
5. 对 BUS 未明确项显式标记“待确认”
