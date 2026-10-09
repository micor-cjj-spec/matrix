# 业务规则

## 状态规则
- 新建单据默认 `DRAFT`
- 仅 `DRAFT / REJECTED` 可提交
- 仅 `SUBMITTED` 可审核或驳回
- 仅 `DRAFT` 可删除

## 特有规则
- 暂估应收允许维护 `fsourceBillNo`
- 审核通过后自动生成并关联凭证
- 当前仍受信用预警硬拦截规则影响
