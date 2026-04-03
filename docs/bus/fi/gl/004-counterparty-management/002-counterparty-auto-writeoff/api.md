# 接口说明

## 预览接口
- `GET /arap-manage/plan`

## 执行接口
- `POST /arap-manage/auto-writeoff`

## 执行参数
- `docTypeRoot`
- `counterparty`
- `asOfDate`
- `operator`

## 返回结构
返回 `Map<String,Object>`：
- `planCode`
- `logId`
- `message`
- `sourceDocCount`
- `targetDocCount`
- `linkCount`
- `totalAmount`
- `records`
- `warnings`

## 前端关联
- 前端 API：`arapManageApi.executeAutoWriteoff`
- 前端页面：`CounterpartyAutoWriteoffView.vue`
