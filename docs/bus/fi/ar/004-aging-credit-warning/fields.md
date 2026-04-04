# 字段设计

## 查询参数
| 参数 | 含义 | 说明 |
|---|---|---|
| docTypeRoot | 业务根类型 | 固定 `AR` |
| asOfDate | 统计日期 | 可空，默认当天 |

## 账龄汇总返回
| 字段 | 含义 |
|---|---|
| docTypeRoot | 业务根类型 |
| asOfDate | 统计日期 |
| docCount | 单据数 |
| totalAmount | 总金额 |
| buckets | 分桶列表 |

## 分桶字段
| 字段 | 含义 |
|---|---|
| range | 区间：`0-30 / 31-60 / 61-90 / 91+` |
| amount | 区间金额 |

## 信用配置字段（BizfiFiCounterpartyCredit）
| 字段 | 含义 |
|---|---|
| fid | 主键 |
| fcounterparty | 往来方 |
| fdocTypeRoot | AR / AP |
| fcreditLimit | 信用额度 |
| foverdueDaysThreshold | 逾期阈值 |
| fenabled | 启用标志 |
| fblockOnOverLimit | 超限硬拦截 |
| fblockOnOverdue | 超期硬拦截 |
| fremark | 备注 |
| fupdatedBy | 更新人 |
| fupdatedTime | 更新时间 |

## 预警字段
| 字段 | 含义 |
|---|---|
| counterparty | 往来方 |
| totalOutstanding | 未结金额 |
| creditLimit | 信用额度 |
| overdueDaysThreshold | 逾期阈值 |
| maxOverdueDays | 最大逾期天数 |
| overLimit | 是否超额度 |
| overdue | 是否超逾期 |
| blockOnOverLimit | 是否超限硬拦截 |
| blockOnOverdue | 是否超期硬拦截 |
