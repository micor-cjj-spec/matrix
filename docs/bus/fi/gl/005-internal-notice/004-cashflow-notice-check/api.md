# 接口说明

## 查询接口
- `GET /internal-notice/cashflow/reconcile`

## 参数
- `orgId`
- `period`
- `status`
- `currency`

## 返回结构
返回 `Map<String,Object>`：
- `rows`
- `warnings`
- `noticeCount`
- `ongoingCount`
- `resolvedCount`
- `snapshotAmount`
- `currentAmount`

## 前端关联
- 前端 API：`internalNoticeApi.fetchCashflowNoticeReconcile`
- 前端页面：`CashflowNoticeCheckView.vue`
