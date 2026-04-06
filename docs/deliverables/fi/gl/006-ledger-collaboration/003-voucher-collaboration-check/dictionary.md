# 凭证协同检查口径说明

## 1. 查询条件口径
- `startDate`：开始日期
- `endDate`：结束日期
- `issueCode`：问题类型
- `severity`：问题等级
- `status`：凭证状态
- `onlyIssue`：仅看异常

## 2. 汇总统计口径
- `voucherCount`：扫描凭证数
- `issueCount`：异常条数
- `issueVoucherCount`：异常凭证数
- `highCount`：高风险问题数
- `healthyCount`：健康凭证数

## 3. 协同检查结果口径
- `rows`：展示凭证、凭证分录、总账分录的一致性检查结果
- 支持识别借贷不平、缺少分录、缺少总账分录、重复凭证号等问题
- 问题类型与问题等级用于体现问题严重程度

## 4. warnings 与跳转口径
- warnings 由后端返回
- 行内可跳转查看凭证

## 5. 待确认项
- 问题类型枚举是否需要独立文档化
- 高风险问题分级规则是否需要更细说明
