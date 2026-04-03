# 字段设计

## 查询参数
| 参数 | 含义 | 说明 |
|---|---|---|
| orgId | 业务单元 | 可空 |
| period | 期间 | 必须为 `yyyy-MM` |
| currency | 币种 | 默认 `CNY` |

## 返回结构
返回 `CashFlowSupplementResultVO`，核心字段包括：
- `postedVoucherCount`
- `cashVoucherCount`
- `cashAccountCount`
- `cashflowItemCount`
- `directCount`
- `heuristicCount`
- `unknownCount`
- `mixedCount`
- `transferCount`
- `cashInAmount`
- `cashOutAmount`
- `netAmount`
- `tasks`
- `categories`
- `pendingVouchers`
- `warnings`

## 任务字段（CashFlowSupplementTaskVO）
| 字段 | 含义 |
|---|---|
| code | 检查项编码 |
| name | 检查项名称 |
| status | 状态：`READY / WARNING / INFO` |
| message | 当前说明 |
| actionHint | 建议动作 |

## 分类字段（CashFlowSupplementCategoryVO）
| 字段 | 含义 |
|---|---|
| categoryCode | 分类编码 |
| categoryName | 分类名称 |
| voucherCount | 凭证数 |
| cashInAmount | 流入 |
| cashOutAmount | 流出 |
| netAmount | 净额 |
| directCount | 直接标记数 |
| heuristicCount | 规则推断数 |
| pendingCount | 待处理数 |

## 待处理凭证字段（CashFlowSupplementVoucherVO）
| 字段 | 含义 |
|---|---|
| voucherId | 凭证ID |
| voucherNumber | 凭证号 |
| voucherDate | 凭证日期 |
| summary | 摘要 |
| sourceType | 识别方式 |
| cashflowItemCode | 现金流项目编码 |
| cashflowItemName | 现金流项目名称 |
| categoryName | 分类名称 |
| netAmount | 净额 |
| issue | 问题描述 |
| suggestion | 建议动作 |
