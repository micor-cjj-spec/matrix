# 接口说明

## 查询接口
- `GET /arap-manage/plan`

## 查询参数
- `docTypeRoot`
- `counterparty`
- `asOfDate`
- `auditedOnly`

## 返回结构
返回 `Map<String,Object>`：
- `records`
- `warnings`
- `sourceDocCount`
- `targetDocCount`
- `counterpartyCount`
- `sourceOpenAmount`
- `targetOpenAmount`
- `suggestedAmount`
- `planCount`
- `remainingSourceAmount`
- `remainingTargetAmount`

## 前端关联
- 前端 API：`arapManageApi.fetchWriteoffPlan`
- 前端页面：`CounterpartyPlanView.vue`
