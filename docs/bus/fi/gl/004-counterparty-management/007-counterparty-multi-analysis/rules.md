# 业务规则

## 查询规则
- `docTypeRoot` 只支持 `AR / AP`。
- `groupDimension` 只支持：`COUNTERPARTY / DOCTYPE / STATUS / COUNTERPARTY_DOCTYPE / ROLE`。
- `asOfDate` 为空时按当天处理。

## 聚合规则
- 原额、已核销、未核销都基于单据级金额累加。
- `avgAgeDays` = 分组内单据账龄平均值，保留两位小数。
- `latestDate` = 分组内最新单据日期。

## 分组规则
- `COUNTERPARTY_DOCTYPE` 的分组名称格式为“往来方 / 单据类型”。
- 未知文本统一回退为 `-`。
