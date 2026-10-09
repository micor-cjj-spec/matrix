# 往来通知单勾稽口径说明

## 1. 查询条件口径
- `docTypeRoot`：往来类型
- `status`：通知状态
- `asOfDate`：统计日期

## 2. 汇总统计口径
- `noticeCount`：通知数
- `ongoingCount`：仍需处理数量
- `resolvedCount`：已自然解除数量
- `snapshotOpenAmount`：通知快照未清金额
- `currentOpenAmount`：当前未清金额

## 3. 勾稽结果口径
- `rows`：展示历史通知与当前最新风险扫描结果的对照关系
- 勾稽结果可反映通知是否仍有效
- 支持对比通知快照未清金额与当前未清金额

## 4. warnings 与跳转口径
- warnings 由后端返回
- 行内可跳转到往来对账单，并根据勾稽结果决定是否带 `openOnly=true`

## 5. 待确认项
- 勾稽状态枚举是否需要独立文档化
- `openOnly=true` 的最终触发规则是否需要更细说明
