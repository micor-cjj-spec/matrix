# 接口说明

## 查询接口
- `GET /arap-manage/writeoff-log`

## 查询参数
- `docTypeRoot`
- `counterparty`
- `planCode`
- `startDate`
- `endDate`

## 返回结构
返回 `Map<String,Object>`：
- `records`
- `warnings`
- `logCount`
- `linkCount`
- `totalAmount`
- `linkDetails`（可选）

## 前端关联
- 前端 API：`arapManageApi.fetchWriteoffLog`
- 前端页面：`CounterpartyWriteoffLogView.vue`
