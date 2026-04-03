# 接口说明

## 查询接口
- `GET /arap-manage/account-query`

## 查询参数
- `docTypeRoot`
- `counterparty`
- `docType`
- `status`
- `openOnly`
- `keyword`
- `asOfDate`

## 返回结构
返回 `Map<String,Object>`：
- `records`
- `warnings`
- `docCount`
- `totalAmount`
- `writtenOffAmount`
- `openAmount`
- `openDocCount`

## 前端关联
- 前端 API：`arapManageApi.fetchCounterpartyAccountQuery`
- 前端页面：`CounterpartyAccountQueryView.vue`
