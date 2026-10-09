# 后端提示词

## 目标

实现关账后的财务期间滚动能力。

## 要求

1. 新增 `BizfiFiPeriodRollover` 实体、Mapper、Service、Controller。
2. 新增 `PeriodRolloverRequestVO` 和 `PeriodRolloverResultVO`。
3. 新增 `POST /period-rollover/from-close-execution/{executionId}`。
4. 新增 `GET /period-rollover/list`。
5. 仅允许基于 `SUCCESS` 且执行后状态为 `CLOSED` 的关账执行记录滚动。
6. 同一关账执行记录不能重复滚动。
7. 自动计算下一期间。
8. 下一期间不存在时调用会计期间服务创建 `OPEN` 期间。
9. 更新组织财务参数 `fcurrentPeriod`。
10. 写入期间滚动记录。
