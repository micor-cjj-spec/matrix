# 字段设计

## 查询参数
| 参数 | 含义 | 说明 |
|---|---|---|
| docTypeRoot | 往来类型 | `AR / AP` |
| groupDimension | 汇总维度 | `COUNTERPARTY / DOCTYPE / STATUS / COUNTERPARTY_DOCTYPE / ROLE` |
| asOfDate | 统计日期 | 可空，默认当天 |

## 返回结构
返回 `Map<String,Object>`，核心字段包括：
- `groupDimension`
- `groupCount`
- `totalAmount`
- `writtenOffAmount`
- `openAmount`
- `rows`
- `warnings`

## 分组行字段
| 字段 | 含义 |
|---|---|
| groupKey | 分组键 |
| groupName | 分组名称 |
| docCount | 单据数 |
| amount | 原额 |
| writtenOffAmount | 已核销金额 |
| openAmount | 未核销金额 |
| avgAgeDays | 平均账龄 |
| latestDate | 最近日期 |
