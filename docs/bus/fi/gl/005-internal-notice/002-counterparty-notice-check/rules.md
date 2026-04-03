# 业务规则

## 勾稽规则
- `status=ALL` 表示不过滤历史通知状态。
- 勾稽依据是历史通知与当前候选的 `referenceKey` 是否一致。
- 历史通知当前找不到对应候选时，视为问题已自然解除。

## 金额规则
- `snapshotOpenAmount` 取自历史通知快照。
- `currentOpenAmount` 取自最新风险扫描结果；若问题已解除则为 0。
- `improvementAmount = snapshotOpenAmount - currentOpenAmount`。
