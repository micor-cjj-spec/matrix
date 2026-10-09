# 字段设计

## 查询参数
| 参数 | 含义 | 说明 |
|---|---|---|
| docTypeRoot | 往来类型 | `AR / AP` |
| counterparty | 往来方 | 可空 |
| asOfDate | 统计日期 | 可空，默认当天 |
| openOnly | 仅看未核销 | 默认 `false` |

## 返回结构
返回 `Map<String,Object>`，核心字段包括：
- `docCount`
- `counterpartyCount`
- `totalAmount`
- `writtenOffAmount`
- `openAmount`
- `openDocCount`
- `recentWriteoffCount`
- `rows`
- `recentLogs`
- `warnings`

## 对账行字段
| 字段 | 含义 |
|---|---|
| fid | 单据ID |
| counterparty | 往来方 |
| docType | 单据类型 |
| number | 单据号 |
| docDate | 单据日期 |
| status | 单据状态 |
| role | 角色：`SOURCE / TARGET / OTHER` |
| amount | 原额 |
| writtenOffAmount | 已核销金额 |
| openAmount | 未核销金额 |
| writeoffStatus | `UNWRITTEN / PARTIAL / FULL` |
| ageDays | 账龄 |
| voucherNumber | 关联凭证号 |

## 最近批次字段
`recentLogs` 为核销日志摘要，核心字段包括：
- `planCode`
- `mode`
- `linkCount`
- `totalAmount`
- `operator`
- `operateTime`
