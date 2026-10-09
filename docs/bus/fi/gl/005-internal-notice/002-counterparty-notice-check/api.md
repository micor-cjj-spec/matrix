# 接口说明

## 查询接口
- `GET /internal-notice/counterparty/reconcile`

## 参数
- `docTypeRoot`
- `status`
- `asOfDate`

## 返回结构
返回 `Map<String,Object>`：
- `rows`
- `warnings`
- `noticeCount`
- `ongoingCount`
- `resolvedCount`
- `snapshotOpenAmount`
- `currentOpenAmount`

## 前端关联
- 前端 API：`internalNoticeApi.fetchCounterpartyNoticeReconcile`
- 前端页面：`CounterpartyNoticeCheckView.vue`
