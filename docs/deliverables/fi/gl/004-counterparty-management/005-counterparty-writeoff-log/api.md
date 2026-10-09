# 往来核销日志接口说明

## 1. 查询接口
- `GET /arap-manage/writeoff-log`

## 2. 查询参数
- `docTypeRoot`
- `counterparty`
- `planCode`
- `startDate`
- `endDate`

## 3. 返回结构
返回 `Map<String,Object>`：
- `records`
- `warnings`
- `logCount`
- `linkCount`
- `totalAmount`
- `linkDetails`（可选）

## 4. 前端关联
- 前端 API：`arapManageApi.fetchWriteoffLog`
- 前端页面：`CounterpartyWriteoffLogView.vue`
- 点击批次行“查看明细”后带回 `planCode`

## 5. 说明
- 当前场景为查询类审计追踪页，不包含写操作接口
- 返回重点在批次摘要、链接明细和 warnings
