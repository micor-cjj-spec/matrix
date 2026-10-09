# 资产负债表接口说明

## 1. 查询接口
- `GET /balance-sheet`
- `GET /balance-sheet/drill`

## 2. 主要参数
- 主表：`orgId / period / currency / templateId / showZero`
- 下钻：`orgId / period / currency / templateId / itemId / itemCode`

## 3. 前端关联
- 前端 API：`financialReportApi.fetchBalanceSheet`
- 前端 API：`financialReportApi.fetchBalanceSheetDrill`
- 前端页面：`BalanceSheetView.vue`

## 4. 当前代码边界
- 前端接口调用已接入
- 本轮未在仓库中检出对应后端实现文件
- 当前文档不展开后端内部 controller / service / mapper 细节

## 5. 说明
- 主表接口用于报表展示与平衡校验
- 下钻接口用于报表项目到会计科目的明细展开
