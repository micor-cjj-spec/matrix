# 往来通知单接口说明

## 1. 查询接口
- `GET /internal-notice/counterparty`

## 2. 生成接口
- `POST /internal-notice/counterparty/generate`

## 3. 参数
- `docTypeRoot`
- `status`
- `severity`
- `asOfDate`
- `operator`（生成接口可选）

## 4. 返回结构
返回 `Map<String,Object>`：
- `rows`
- `warnings`
- `noticeCount`
- `openCount`
- `resolvedCount`
- `highCount`
- `amount`
- `openAmount`
- 生成时额外返回：`generatedCount / insertedCount / updatedCount / resolvedAutoCount / message`

## 5. 前端关联
- 前端 API：`internalNoticeApi.fetchCounterpartyNotices`
- 前端 API：`internalNoticeApi.generateCounterpartyNotices`
- 前端页面：`CounterpartyNoticeView.vue`

## 6. 说明
- 当前场景包含查询与生成两类能力
- 生成接口成功后会更新通知单表中的通知状态与快照信息
