# 往来自动核销接口说明

## 1. 预览接口
- `GET /arap-manage/plan`

## 2. 执行接口
- `POST /arap-manage/auto-writeoff`

## 3. 执行参数
- `docTypeRoot`
- `counterparty`
- `asOfDate`
- `operator`

## 4. 返回结构
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

## 5. 前端关联
- 前端 API：`arapManageApi.executeAutoWriteoff`
- 前端页面：`CounterpartyAutoWriteoffView.vue`
- 初始化可从方案页带入条件并自动预览

## 6. 说明
- 当前场景包含预览与执行两类能力
- 执行接口成功后会产生真实核销结果并落库
