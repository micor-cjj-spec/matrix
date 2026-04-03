# 接口说明

## 查询接口
- `GET /internal-notice/counterparty`

## 生成接口
- `POST /internal-notice/counterparty/generate`

## 参数
- `docTypeRoot`
- `status`
- `severity`
- `asOfDate`
- `operator`（生成接口可选）

## 返回结构
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

## 前端关联
- 前端 API：`internalNoticeApi.fetchCounterpartyNotices`
- 前端 API：`internalNoticeApi.generateCounterpartyNotices`
- 前端页面：`CounterpartyNoticeView.vue`
