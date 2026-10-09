# 业务规则

## 查询规则
- `docTypeRoot` 只支持 `AR / AP`。
- `asOfDate` 为空时按当天处理。
- `docType`、`status` 都是精确匹配。
- `keyword` 为模糊匹配，范围包括单据号、往来方、单据类型、状态、凭证号、备注。

## 状态规则
- `writeoffStatus` 计算规则与对账单一致：`UNWRITTEN / PARTIAL / FULL`。
- 不限制单据状态时，后端允许查询更多状态，而不仅是可核销状态。
