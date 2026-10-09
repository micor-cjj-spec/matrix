# 往来通知单勾稽接口说明

## 1. 查询接口
- `GET /internal-notice/counterparty/reconcile`

## 2. 查询参数
- `docTypeRoot`
- `status`
- `asOfDate`

## 3. 返回结构
返回 `Map<String,Object>`：
- `rows`
- `warnings`
- `noticeCount`
- `ongoingCount`
- `resolvedCount`
- `snapshotOpenAmount`
- `currentOpenAmount`

## 4. 前端关联
- 前端 API：`internalNoticeApi.fetchCounterpartyNoticeReconcile`
- 前端页面：`CounterpartyNoticeCheckView.vue`

## 5. 说明
- 当前场景为查询类勾稽页面，不包含写操作接口
- 返回重点在勾稽结果、warnings 与汇总统计字段
