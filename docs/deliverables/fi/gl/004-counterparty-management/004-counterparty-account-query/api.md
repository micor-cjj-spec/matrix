# 往来账查询接口说明

## 1. 查询接口
- `GET /arap-manage/account-query`

## 2. 查询参数
- `docTypeRoot`
- `counterparty`
- `docType`
- `status`
- `openOnly`
- `keyword`
- `asOfDate`

## 3. 返回结构
返回 `Map<String,Object>`：
- `records`
- `warnings`
- `docCount`
- `totalAmount`
- `writtenOffAmount`
- `openAmount`
- `openDocCount`

## 4. 前端关联
- 前端 API：`arapManageApi.fetchCounterpartyAccountQuery`
- 前端页面：`CounterpartyAccountQueryView.vue`

## 5. 说明
- 当前场景为查询类单据追踪页，不包含执行类接口
- 返回重点在单据级结果、warnings 与汇总统计字段
