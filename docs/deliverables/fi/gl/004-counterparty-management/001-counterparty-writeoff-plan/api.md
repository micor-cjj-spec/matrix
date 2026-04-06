# 往来核销方案接口说明

## 1. 查询接口
- `GET /arap-manage/plan`

## 2. 查询参数
- `docTypeRoot`
- `counterparty`
- `asOfDate`
- `auditedOnly`

## 3. 返回结构
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

## 4. 前端关联
- 前端 API：`arapManageApi.fetchWriteoffPlan`
- 前端页面：`CounterpartyPlanView.vue`
- 点击“去自动核销”跳转自动核销页

## 5. 说明
- 当前场景为查询类方案预览页，不包含落库写操作
- 返回重点在方案记录、warnings 与方案统计字段
