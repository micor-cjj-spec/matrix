# 现金流通知单勾稽接口说明

## 1. 查询接口
- `GET /internal-notice/cashflow/reconcile`

## 2. 查询参数
- `orgId`
- `period`
- `status`
- `currency`

## 3. 返回结构
返回 `Map<String,Object>`：
- `rows`
- `warnings`
- `noticeCount`
- `ongoingCount`
- `resolvedCount`
- `snapshotAmount`
- `currentAmount`

## 4. 前端关联
- 前端 API：`internalNoticeApi.fetchCashflowNoticeReconcile`
- 前端页面：`CashflowNoticeCheckView.vue`

## 5. 说明
- 当前场景为查询类勾稽页面，不包含写操作接口
- 返回重点在勾稽结果、warnings 与汇总统计字段
