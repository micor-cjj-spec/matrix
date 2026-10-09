# BOTP V3 反向反写与失败补偿

## 目标

在目标单作废、服务中断或反写失败后，保证 BOTP 可以恢复到正确业务状态，并且不重复创建目标单。

## 核心原则

1. 源单已申请金额始终按 `ACTIVE` 关系金额求和重算，禁止增量加减。
2. 目标单生命周期事件以 `eventId` 幂等处理。
3. 目标单创建成功后，只允许补关系和反写，不允许重新建单。
4. 业务作废先提交；BOTP 通知失败由自动对账补偿，不使用跨服务分布式事务回滚业务事实。
5. 反写任务通过数据库状态条件更新抢占，多实例下同一任务只执行一次。

## 正向失败恢复

```text
TARGET_CREATED
  → RELATION_SAVING
  → RELATION_SAVED
  → WRITEBACK_PROCESSING
  → WRITEBACK_PENDING
  → 自动/人工重试
  → SUCCEEDED
```

`WRITEBACK_PENDING` 创建持久化任务。重试只调用源单反写适配器，不调用目标单创建适配器。

## 目标单作废

```text
付款申请作废
  → fi-service 提交 VOID
  → 通知 BOTP target status event
  → ACTIVE 关系改为 INVALID
  → 汇总剩余 ACTIVE 关系金额
  → 创建 REVERSE_WRITEBACK 任务
  → 源单重算
  → 关系改为 REVERSED
```

## 重试策略

- 第 1 次：10 秒
- 第 2 次：30 秒
- 第 3 次：2 分钟
- 第 4 次：10 分钟
- 第 5 次：30 分钟
- 超过最大次数：`DEAD`

## 自动对账

当前 V3 检查：

- 关系台账有效金额与应付单已申请金额不一致。
- 付款申请已作废，但关系仍是 `ACTIVE`。

支持自动修复、人工修复和填写原因后忽略。
