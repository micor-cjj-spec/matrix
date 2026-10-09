# 凭证汇总表统计口径说明

## 1. 查询条件口径
- `startDate`：统计起始日期，按凭证日期过滤
- `endDate`：统计结束日期，按凭证日期过滤
- `status`：按凭证状态过滤
- `summaryKeyword`：按凭证摘要关键字过滤

## 2. 汇总卡片口径
- `totalCount`：符合当前条件的凭证总数
- `totalAmount`：符合当前条件的凭证金额汇总
- `draftCount` / `submittedCount` / `auditedCount` / `postedCount` / `rejectedCount` / `reversedCount`：各状态数量统计
- `postedAmount`：已过账凭证金额汇总

## 3. 表格行口径
- 每行按日期聚合
- 行内统计当天凭证数量、金额以及状态分布
- 行级“查看凭证”用于反查当天对应凭证列表

## 4. warnings 口径
- warnings 由后端聚合逻辑输出
- 用于提示异常、积压或需要关注的统计现象
- 展示口径必须与后端返回保持一致，不在前端自行推导

## 5. 待确认项
- `totalAmount` 是否包含全部状态金额还是仅统计有效状态
- `warnings` 的具体触发规则是否需要业务侧进一步固化
