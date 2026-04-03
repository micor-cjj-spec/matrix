# 接口说明

## 查询
- `GET /internal-notice/cashflow`

## 生成
- `POST /internal-notice/cashflow/generate`

## 主要参数
- `orgId`
- `period`
- `status`
- `severity`
- `sourceCode`
- `currency`

## 返回
- `rows`
- `warnings`
- `noticeCount`
- `openCount`
- `resolvedCount`
- `highCount`
- `amount`

## 前端关联
- `internalNoticeApi.fetchCashflowNotices`
- `internalNoticeApi.generateCashflowNotices`
- `CashflowNoticeView.vue`
