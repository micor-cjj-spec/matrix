# 现金流通知单勾稽口径说明

## 1. 查询条件口径
- `orgId`：业务单元
- `period`：期间
- `status`：通知状态
- `currency`：币种

## 2. 汇总统计口径
- `noticeCount`：通知数
- `ongoingCount`：仍需处理数量
- `resolvedCount`：已自然解除数量
- `snapshotAmount`：通知快照金额
- `currentAmount`：当前金额

## 3. 勾稽结果口径
- `rows`：展示历史通知与当前最新现金流候选结果的对照关系
- 勾稽结果可反映通知是否仍有效
- 支持对比通知快照金额与当前问题金额

## 4. warnings 与跳转口径
- warnings 由后端返回
- 行内可跳转到凭证页查看具体凭证

## 5. 待确认项
- 勾稽状态枚举是否需要独立文档化
- 当前金额的计算范围是否需要更细说明
