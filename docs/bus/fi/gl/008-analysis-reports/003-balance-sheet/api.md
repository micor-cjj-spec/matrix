# 接口说明

## 查询接口
- `GET /balance-sheet`
- `GET /balance-sheet/drill`

## 主要参数
- 主表：`orgId / period / currency / templateId / showZero`
- 下钻：`orgId / period / currency / templateId / itemId / itemCode`

## 前端关联
- 前端 API：`financialReportApi.fetchBalanceSheet`
- 前端 API：`financialReportApi.fetchBalanceSheetDrill`
- 前端页面：`BalanceSheetView.vue`

## 代码现状说明
- 前端接口调用已接入。
- 本轮未在仓库中检出对应后端实现文件。
