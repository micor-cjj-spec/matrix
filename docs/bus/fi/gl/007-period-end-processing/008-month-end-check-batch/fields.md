# 字段设计

## 批次主字段

| 字段 | 含义 | 类型 | 是否必填 | 备注 |
|---|---|---|---|---|
| fid | 主键 | Long | 是 | 自增 |
| fbatchNo | 批次号 | String | 是 | 系统生成 |
| forg | 业务单元 | Long | 否 | 与工作台检查口径一致 |
| fperiod | 会计期间 | String | 是 | `yyyy-MM` |
| fcloseStatus | 关账准备状态 | String | 是 | `READY` / `WARNING` / `BLOCKED` / `CLOSED` |
| freadinessScore | 准备度分数 | Integer | 是 | 0-100 |
| fcanClose | 是否建议关账 | Boolean | 是 | 来自工作台 |
| fblockingCount | 阻塞项数量 | Integer | 是 | 来自工作台 |
| fwarningCount | 预警项数量 | Integer | 是 | 来自工作台 |
| fpendingCount | 待确认数量 | Integer | 是 | 来自工作台 |
| fpassedCount | 通过数量 | Integer | 是 | 来自工作台 |
| ftotalCheckCount | 检查项总数 | Integer | 是 | 来自工作台 |
| fapplicationStatus | 申请状态 | String | 是 | `DRAFT` / `SUBMITTED` / `APPROVED` |
| fsnapshotJson | 检查快照 | Text | 是 | 保存工作台返回 JSON |
| fremark | 备注 | String | 否 | 人工说明 |

## 状态字段

| 状态 | 含义 |
|---|---|
| DRAFT | 已生成检查批次，未提交关账申请 |
| SUBMITTED | 已提交关账申请 |
| APPROVED | 申请已批准，后续可执行正式关账 |

