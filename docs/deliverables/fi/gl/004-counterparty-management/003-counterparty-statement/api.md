# 往来对账单接口说明

## 1. 查询接口
- `GET /arap-manage/statement`

## 2. 查询参数
- `docTypeRoot`
- `counterparty`
- `asOfDate`
- `openOnly`

## 3. 返回结构
返回 `Map<String,Object>`：
- `rows`
- `recentLogs`
- `warnings`
- `docCount`
- `counterpartyCount`
- `totalAmount`
- `writtenOffAmount`
- `openAmount`
- `openDocCount`
- `recentWriteoffCount`

## 4. 前端关联
- 前端 API：`arapManageApi.fetchCounterpartyStatement`
- 前端页面：`CounterpartyStatementView.vue`
- 对账明细可跳转关联凭证
- 最近批次可跳转核销日志

## 5. 说明
- 当前场景为查询类对账报表，不包含写操作接口
- 返回重点在单据级对账明细、最近批次摘要和 warnings
