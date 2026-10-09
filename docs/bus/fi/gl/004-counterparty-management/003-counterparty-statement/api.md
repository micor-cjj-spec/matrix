# 接口说明

## 查询接口
- `GET /arap-manage/statement`

## 查询参数
- `docTypeRoot`
- `counterparty`
- `asOfDate`
- `openOnly`

## 返回结构
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

## 前端关联
- 前端 API：`arapManageApi.fetchCounterpartyStatement`
- 前端页面：`CounterpartyStatementView.vue`
