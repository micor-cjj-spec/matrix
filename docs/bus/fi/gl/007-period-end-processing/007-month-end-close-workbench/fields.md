# 字段设计

## 查询字段

| 字段 | 含义 | 类型 | 是否必填 | 备注 |
|---|---|---|---|---|
| forg | 业务单元 ID | Long | 否 | 为空时按系统可识别范围检查，推荐前端默认取第一个业务单元 |
| period | 会计期间 | String | 否 | 格式 `yyyy-MM`；为空时按组织财务参数或系统月份解析 |

## 返回主字段

| 字段 | 含义 | 类型 | 备注 |
|---|---|---|---|
| forg | 业务单元 ID | Long | 与查询口径一致 |
| period | 解析后的期间 | String | 格式 `yyyy-MM` |
| periodSource | 期间来源 | String | `PARAM` / `ORG_CONFIG` / `SYSTEM` |
| baseCurrency | 本位币 | String | 来自组织财务参数 |
| currentPeriod | 组织当前期间 | String | 来自组织财务参数 |
| periodStatus | 会计期间状态 | String | 例如 `OPEN` / `CLOSED` / `MISSING` |
| closeStatus | 关账准备状态 | String | `READY` / `WARNING` / `BLOCKED` / `CLOSED` |
| readinessScore | 准备度分数 | Integer | 0-100 |
| canClose | 是否建议关账 | Boolean | 首版只判断，不执行 |
| blockingCount | 阻塞项数量 | Integer | 严重阻塞项 |
| warningCount | 预警项数量 | Integer | 非阻塞但需关注 |
| passedCount | 通过项数量 | Integer | 已通过检查项 |
| totalCheckCount | 检查项总数 | Integer | 阻塞、预警、通过和待确认合计 |

## 检查项字段

| 字段 | 含义 | 类型 | 备注 |
|---|---|---|---|
| code | 检查项编码 | String | 前端用作唯一键 |
| name | 检查项名称 | String | 面向财务用户展示 |
| category | 检查类别 | String | `FOUNDATION` / `VOUCHER` / `PERIOD_END` / `REPORT` / `CLOSE` |
| status | 检查状态 | String | `PASSED` / `WARNING` / `BLOCKED` / `PENDING` |
| severity | 严重级别 | String | `LOW` / `MEDIUM` / `HIGH` |
| message | 检查说明 | String | 当前结果描述 |
| actionHint | 建议动作 | String | 引导用户下一步处理 |
| routePath | 下钻路径 | String | 对应前端路由 |
| relatedCount | 关联数量 | Integer | 例如未过账凭证数、问题数 |
| blocking | 是否阻塞关账 | Boolean | `true` 表示不建议关账 |

