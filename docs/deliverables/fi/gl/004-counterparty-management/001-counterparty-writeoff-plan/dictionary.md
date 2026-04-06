# 往来核销方案口径说明

## 1. 查询条件口径
- `docTypeRoot`：往来类型
- `counterparty`：往来方
- `asOfDate`：统计日期
- `auditedOnly`：仅已审核

## 2. 汇总统计口径
- `sourceDocCount`：源单据数
- `targetDocCount`：结算单据数
- `counterpartyCount`：往来方数
- `sourceOpenAmount`：源单据待核销金额
- `targetOpenAmount`：结算单据待分配金额
- `suggestedAmount`：建议核销金额
- `planCount`：方案条数
- `remainingSourceAmount`：方案后源单据剩余
- `remainingTargetAmount`：方案后结算单据剩余

## 3. 方案记录口径
- 展示源单据、结算单据、建议核销金额、剩余金额等匹配建议
- 当前仅为预览方案，不代表已执行核销

## 4. warnings 与跳转口径
- warnings 由后端返回
- “去自动核销”跳转到自动核销页并沿用当前查询条件

## 5. 待确认项
- 方案排序与匹配优先级是否需要更细说明
- warnings 的正式触发规则是否需要单独固化
