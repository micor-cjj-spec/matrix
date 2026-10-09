# 字段设计

## 查询参数
| 参数 | 含义 | 说明 |
|---|---|---|
| forg | 业务单元ID | 可空；前端从业务单元下拉中选择 |
| period | 期间 | 格式建议 `yyyy-MM` |

## 返回头字段（VoucherCarryListResultVO）
| 字段 | 含义 |
|---|---|
| forg | 业务单元ID |
| period | 当前解析后的期间 |
| periodSource | 期间来源：`PARAM / ORG_CONFIG / SYSTEM` |
| baseCurrency | 本位币 |
| currentPeriod | 组织当前期间 |
| periodStatus | 会计期间状态 |
| defaultVoucherType | 默认凭证字 |
| foundationHealthy | 基础资料是否健康 |
| periodVoucherCount | 本期全部凭证数 |
| carryVoucherCount | 识别到的结转相关凭证数 |
| periodVoucherAmount | 本期全部凭证金额 |
| tasks | 检查任务清单 |
| relatedVouchers | 结转相关凭证列表 |
| warnings | 页面提示信息 |

## 任务字段（VoucherCarryTaskVO）
| 字段 | 含义 |
|---|---|
| code | 检查项编码 |
| name | 检查项名称 |
| status | 任务状态：`READY / WARNING / PENDING / DONE` |
| message | 当前说明 |
| actionHint | 建议动作 |

## 关联凭证字段
`relatedVouchers` 当前直接返回 `BizfiFiVoucher`，页面展示字段主要包括：
- `fnumber`
- `fdate`
- `fsummary`
- `famount`
- `fstatus`
