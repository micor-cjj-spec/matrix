# 利润表接口说明

## 1. 查询接口
- `GET /profit-statement`

## 2. 主要参数
- `orgId`
- `startPeriod`
- `endPeriod`
- `currency`
- `templateId`
- `showZero`

## 3. 前端关联
- 前端 API：`financialReportApi.fetchProfitStatement`
- 前端页面：`ProfitStatementView.vue`

## 4. 当前代码边界
- 前端接口调用已接入
- 本轮未在仓库中检出对应后端实现文件
- 当前文档不展开后端内部 controller / service / mapper 细节

## 5. 说明
- 当前接口用于利润表主表查询
- 当前页面未接入项目下钻弹窗
